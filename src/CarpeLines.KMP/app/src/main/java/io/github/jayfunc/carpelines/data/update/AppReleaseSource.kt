package io.github.jayfunc.carpelines.data.update

import android.os.Build
import io.github.jayfunc.carpelines.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Looks up the newest published build of each channel. All channels read the same GitHub
 * releases feed: stable builds are tagged releases, alpha and beta builds are prereleases.
 */
@Singleton
class AppReleaseSource @Inject constructor(
    baseClient: OkHttpClient,
) {
    private val client = baseClient.newBuilder()
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", "CarpeLines/${BuildConfig.VERSION_NAME} ($PROJECT_URL)")
                    .build()
            )
        }
        .build()

    /** Newest release of [channel], or null when the channel has nothing installable for this device. */
    suspend fun latest(channel: UpdateChannel): AppRelease? = withContext(Dispatchers.IO) {
        parseLatest(
            JSONArray(getBody(GITHUB_RELEASES_API)),
            channel,
            supportedAbis = Build.SUPPORTED_ABIS.toList(),
        )
    }

    private fun getBody(url: String): String {
        val request = Request.Builder().url(url).header("Accept", "application/json").get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} from $url")
            return response.body.string()
        }
    }

    companion object {
        private const val PROJECT_URL = "https://github.com/jayfunc/CarpeLines"

        /** Release applicationId; debug builds add a suffix but should still see real releases. */
        internal const val PUBLISHED_PACKAGE = "io.github.jayfunc.carpelines"
        private const val GITHUB_RELEASES_API =
            "https://api.github.com/repos/jayfunc/CarpeLines/releases?per_page=30"

        /**
         * Newest release of [channel] with an APK for this device. CI tags releases
         * `v<version>-<channel>.<build>-<hash>` and names assets
         * `CarpeLines-<version>-<channel>.<build>-<hash>-<abi|universal>.apk`; legacy tags such
         * as `v0.5.1` and `v0.3.0-alpha.16` still resolve to their channel.
         */
        internal fun parseLatest(
            releases: JSONArray,
            channel: UpdateChannel,
            supportedAbis: List<String>,
        ): AppRelease? {
            val expectPrerelease = channel != UpdateChannel.STABLE
            return (0 until releases.length())
                .mapNotNull { releases.optJSONObject(it) }
                .filter { !it.optBoolean("draft") }
                .filter { it.optBoolean("prerelease") == expectPrerelease }
                .mapNotNull { release ->
                    val tag = release.optString("tag_name")
                    val version = BuildVersion.parse(tag) ?: return@mapNotNull null
                    // Legacy stable tags predate the channel token.
                    if ((version.channel ?: UpdateChannel.STABLE) != channel) return@mapNotNull null
                    val asset = pickAsset(release.optJSONArray("assets"), supportedAbis)
                        ?: return@mapNotNull null
                    version to AppRelease(
                        channel = channel,
                        versionName = tag.removePrefix("v"),
                        versionCode = null,
                        apkUrl = asset.optString("browser_download_url"),
                        apkSizeBytes = asset.optLong("size", -1L).takeIf { it > 0 },
                        pageUrl = release.optString("html_url").ifBlank { "$PROJECT_URL/releases/tag/$tag" },
                    )
                }
                .maxByOrNull { (version, _) -> version }
                ?.second
        }

        private fun pickAsset(assets: JSONArray?, supportedAbis: List<String>): JSONObject? {
            if (assets == null) return null
            val apks = (0 until assets.length())
                .mapNotNull { assets.optJSONObject(it) }
                .filter {
                    it.optString("name").endsWith(".apk") && it.optString("browser_download_url").isNotBlank()
                }
            return supportedAbis.firstNotNullOfOrNull { abi ->
                apks.firstOrNull { it.optString("name").endsWith("-$abi.apk") }
            } ?: apks.firstOrNull { it.optString("name").endsWith("-universal.apk") }
        }
    }
}
