package io.github.jayfunc.carpelines.data.update

/**
 * Where builds of a channel are published. All channels live on GitHub Releases: stable builds
 * are normal releases, alpha and beta builds are prereleases. CI signs every channel with the
 * same key, but the updater only installs within the selected channel; moving between channels
 * needs the user-driven reinstall flow.
 */
enum class UpdateChannel(val storageKey: String) {
    STABLE("stable"),
    BETA("beta"),
    ALPHA("alpha");

    companion object {
        fun fromStorageKey(key: String?): UpdateChannel? = entries.firstOrNull { it.storageKey == key }

        /**
         * The channel the running build was distributed through, encoded in its version name
         * (`1.0.0-beta.57-3f9a1c2`). Legacy stable names carry no channel token, while legacy CI
         * and PR builds (which have always used a `-` suffix) count as alpha.
         */
        fun ofVersionName(versionName: String): UpdateChannel =
            BuildVersion.parse(versionName)?.channel
                ?: if ('-' in versionName) ALPHA else STABLE
    }
}

/**
 * A published build, as it appears in a release tag or the installed version name:
 * `<major>.<minor>.<patch>[-<channel>.<build>][-<hash>]`, for example `1.0.0-beta.57-3f9a1c2`.
 *
 * Legacy names `0.5.1` (stable) and `0.3.0-alpha.16` (alpha) parse as well, so an installed app
 * can still compare itself against builds from the old pipelines.
 */
internal data class BuildVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val channel: UpdateChannel?,
    val build: Int,
    val hash: String?,
) : Comparable<BuildVersion> {

    override fun compareTo(other: BuildVersion): Int = compareValuesBy(
        this,
        other,
        BuildVersion::major,
        BuildVersion::minor,
        BuildVersion::patch,
        BuildVersion::build,
        { it.hash.orEmpty() },
    )

    companion object {
        private val PATTERN = Regex(
            """^v?(\d+)\.(\d+)\.(\d+)(?:-(alpha|beta|stable|dev)\.(\d+))?(?:-([0-9a-f]{7,40}))?$"""
        )

        fun parse(value: String): BuildVersion? {
            val groups = PATTERN.matchEntire(value.trim())?.groupValues ?: return null
            return BuildVersion(
                major = groups[1].toIntOrNull() ?: return null,
                minor = groups[2].toIntOrNull() ?: return null,
                patch = groups[3].toIntOrNull() ?: return null,
                channel = when (groups[4]) {
                    UpdateChannel.ALPHA.storageKey -> UpdateChannel.ALPHA
                    UpdateChannel.BETA.storageKey -> UpdateChannel.BETA
                    UpdateChannel.STABLE.storageKey -> UpdateChannel.STABLE
                    else -> null
                },
                build = groups[5].toIntOrNull() ?: 0,
                hash = groups[6].takeIf { it.isNotEmpty() },
            )
        }
    }
}

/** A published build that can be offered to the user. */
data class AppRelease(
    val channel: UpdateChannel,
    val versionName: String,
    /** Releases expose only their version name; install verification reads the APK itself. */
    val versionCode: Long?,
    val apkUrl: String,
    val apkSizeBytes: Long?,
    val pageUrl: String,
)

/** Whether [release] is newer than the running build, assuming both are on the same channel. */
internal fun AppRelease.isNewerThan(installedVersionName: String, installedVersionCode: Long): Boolean {
    val candidate = BuildVersion.parse(versionName) ?: return false
    val installed = BuildVersion.parse(installedVersionName)
        // A PR or hand-built install has no comparable version; any published release is the
        // way back onto a channel.
        ?: return true
    if (installed.channel != null && installed.channel != channel) return true
    // Legacy stable names have no channel token; they are only comparable with stable builds.
    if (installed.channel == null && channel != UpdateChannel.STABLE) return true
    return candidate > installed
}
