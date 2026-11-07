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
         * (`1.0.0-beta.57`). Legacy stable names carry no channel token, while legacy CI and PR
         * builds (which have always used a `-` suffix) count as alpha.
         */
        fun ofVersionName(versionName: String): UpdateChannel =
            BuildVersion.parse(versionName)?.channel
                ?: if ('-' in versionName) ALPHA else STABLE
    }
}

/**
 * A published build version, as it appears in a release tag, an APK asset name, or the installed
 * version name: `<major>.<minor>.<patch>[-<channel>.<build-or-stamp>][-<hash>]`, for example
 * `1.0.0-beta.57` or the stamped tag `1.0.0-beta.20261106.1357`. A stamp is the commit time
 * (`yyyyMMdd.HHmm`, UTC) flattened to digits, so tagged builds sort chronologically; both forms
 * compare numerically.
 *
 * Legacy names `0.5.1` (stable) and `0.3.0-alpha.16` (alpha) parse as well, so an installed app
 * can still compare itself against builds from the old pipelines.
 */
internal data class BuildVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val channel: UpdateChannel?,
    /** Commit count, or a commit-time stamp flattened to digits (`202611061357`) in release tags. */
    val build: Long,
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
            """^v?(\d+)\.(\d+)\.(\d+)(?:-(alpha|beta|stable|dev)\.(\d+(?:\.\d+)?))?(?:-([0-9a-f]{7,40}))?$"""
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
                build = groups[5].replace(".", "").toLongOrNull() ?: 0L,
                hash = groups[6].takeIf { it.isNotEmpty() },
            )
        }
    }
}

private val COMMIT_HASH_SUFFIX = Regex("""-[0-9a-f]{7,40}$""")

/** Strips the `-<commit>` suffix that older releases and installs carry in their version names. */
internal fun String.withoutCommitHash(): String = replace(COMMIT_HASH_SUFFIX, "")

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
