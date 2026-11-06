package io.github.jayfunc.carpelines.data.update

/**
 * Where builds of a channel are published. Both channels live on GitHub Releases: stable builds
 * are the tagged releases, alpha builds are prereleases signed with the project CI key, so an
 * install from one channel can never be updated in place by the other.
 */
enum class UpdateChannel(val storageKey: String) {
    STABLE("stable"),
    ALPHA("alpha");

    companion object {
        fun fromStorageKey(key: String?): UpdateChannel? = entries.firstOrNull { it.storageKey == key }

        /**
         * The channel the running build was distributed through. CI-built alphas (and PR builds,
         * which share the CI key) carry a pre-release suffix; releases use the plain tag.
         */
        fun ofVersionName(versionName: String): UpdateChannel =
            if ('-' in versionName) ALPHA else STABLE
    }
}

/** `0.3.0-alpha.16` as published by the alpha-release workflow. */
data class AlphaVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val build: Int,
) : Comparable<AlphaVersion> {

    override fun compareTo(other: AlphaVersion): Int = compareValuesBy(
        this, other, AlphaVersion::major, AlphaVersion::minor, AlphaVersion::patch, AlphaVersion::build
    )

    companion object {
        private val PATTERN = Regex("""^v?(\d+)\.(\d+)\.(\d+)-alpha\.(\d+)$""")

        fun parse(value: String): AlphaVersion? {
            val groups = PATTERN.matchEntire(value.trim())?.groupValues ?: return null
            return AlphaVersion(
                major = groups[1].toIntOrNull() ?: return null,
                minor = groups[2].toIntOrNull() ?: return null,
                patch = groups[3].toIntOrNull() ?: return null,
                build = groups[4].toIntOrNull() ?: return null,
            )
        }
    }
}

/** `0.5.1` — the tag of a stable GitHub release. */
data class StableVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<StableVersion> {

    override fun compareTo(other: StableVersion): Int = compareValuesBy(
        this, other, StableVersion::major, StableVersion::minor, StableVersion::patch
    )

    companion object {
        private val PATTERN = Regex("""^v?(\d+)\.(\d+)\.(\d+)$""")

        fun parse(value: String): StableVersion? {
            val groups = PATTERN.matchEntire(value.trim())?.groupValues ?: return null
            return StableVersion(
                major = groups[1].toIntOrNull() ?: return null,
                minor = groups[2].toIntOrNull() ?: return null,
                patch = groups[3].toIntOrNull() ?: return null,
            )
        }
    }
}

/** A published build that can be offered to the user. */
data class AppRelease(
    val channel: UpdateChannel,
    val versionName: String,
    /** alpha releases only expose their version name. */
    val versionCode: Long?,
    val apkUrl: String,
    val apkSizeBytes: Long?,
    val pageUrl: String,
)

/** Whether [release] is newer than the running build, assuming both are on the same channel. */
internal fun AppRelease.isNewerThan(installedVersionName: String, installedVersionCode: Long): Boolean =
    when (channel) {
        UpdateChannel.STABLE -> {
            // GitHub releases carry no version code; the tag is the only truth.
            val candidate = StableVersion.parse(versionName) ?: return false
            val installed = StableVersion.parse(installedVersionName)
                // A non-tag install (CI or hand-built) has no stable number; any published
                // release is the way back onto the channel.
                ?: return true
            candidate > installed
        }
        UpdateChannel.ALPHA -> {
            val candidate = AlphaVersion.parse(versionName) ?: return false
            // PR and other CI builds share the alpha signing key but have no alpha number;
            // any published alpha is the way back onto the channel.
            val installed = AlphaVersion.parse(installedVersionName) ?: return true
            candidate > installed
        }
    }
