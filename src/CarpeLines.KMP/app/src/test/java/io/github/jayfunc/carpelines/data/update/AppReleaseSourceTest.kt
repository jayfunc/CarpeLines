package io.github.jayfunc.carpelines.data.update

import com.google.common.truth.Truth.assertThat
import org.json.JSONArray
import org.junit.jupiter.api.Test

class AppReleaseSourceTest {

    // region JSON builders (GitHub API payloads)

    private fun releaseJson(
        tag: String,
        prerelease: Boolean,
        draft: Boolean,
        assetsJson: String,
    ): String =
        """{"tag_name":"$tag","prerelease":$prerelease,"draft":$draft,
           "html_url":"https://github.com/jayfunc/CarpeLines/releases/tag/$tag","assets":$assetsJson}"""

    private fun alphaReleaseJson(
        tag: String,
        prerelease: Boolean = true,
        draft: Boolean = false,
        abis: List<String> = listOf("arm64-v8a", "armeabi-v7a"),
    ): String {
        val name = tag.removePrefix("v")
        val assets = abis.joinToString(prefix = "[", postfix = "]") { abi ->
            """{"name":"CarpeLines-$name-$abi.apk","size":37082259,
               "browser_download_url":"https://github.com/jayfunc/CarpeLines/releases/download/$tag/CarpeLines-$name-$abi.apk"}"""
        }
        return releaseJson(tag, prerelease, draft, assets)
    }

    private fun stableReleaseJson(
        tag: String,
        draft: Boolean = false,
        assets: List<Pair<String, String>> = listOf(
            "CarpeLines-${tag.removePrefix("v")}-universal.apk" to
                "https://github.com/jayfunc/CarpeLines/releases/download/$tag/CarpeLines-${tag.removePrefix("v")}-universal.apk"
        ),
    ): String {
        val assetsJson = assets.joinToString(prefix = "[", postfix = "]") { (name, url) ->
            """{"name":"$name","size":37082259,"browser_download_url":"$url"}"""
        }
        return releaseJson(tag, prerelease = false, draft = draft, assetsJson = assetsJson)
    }

    // endregion

    // region AppRelease builders

    private fun alphaAppRelease(versionName: String) = AppRelease(
        channel = UpdateChannel.ALPHA,
        versionName = versionName,
        versionCode = null,
        apkUrl = "https://example.invalid/app.apk",
        apkSizeBytes = null,
        pageUrl = "https://example.invalid",
    )

    private fun stableAppRelease(versionName: String) = AppRelease(
        channel = UpdateChannel.STABLE,
        versionName = versionName,
        versionCode = null,
        apkUrl = "https://example.invalid/app.apk",
        apkSizeBytes = null,
        pageUrl = "https://example.invalid",
    )

    // endregion

    @Test
    fun `newest stable release is picked by version even when the API lists it later`() {
        val releases = JSONArray(
            listOf(
                stableReleaseJson("v0.4.2"),
                stableReleaseJson("v0.5.0"),
                stableReleaseJson("v0.4.10"),
            ).joinToString(prefix = "[", postfix = "]")
        )

        val latest = AppReleaseSource.parseLatestStable(releases)!!

        assertThat(latest.versionName).isEqualTo("0.5.0")
        assertThat(latest.channel).isEqualTo(UpdateChannel.STABLE)
        assertThat(latest.apkUrl).endsWith("CarpeLines-0.5.0-universal.apk")
    }

    @Test
    fun `stable asset prefers the universal APK over architecture-specific ones`() {
        val releases = JSONArray(
            """[${stableReleaseJson(
                "v0.5.0",
                assets = listOf(
                    "CarpeLines-0.5.0-arm64-v8a.apk" to "https://example.invalid/arm64.apk",
                    "CarpeLines-0.5.0-universal.apk" to "https://example.invalid/universal.apk",
                )
            )}]"""
        )

        val latest = AppReleaseSource.parseLatestStable(releases)!!

        assertThat(latest.apkUrl).isEqualTo("https://example.invalid/universal.apk")
    }

    @Test
    fun `prereleases, drafts and releases without an APK are not offered as stable`() {
        val releases = JSONArray(
            listOf(
                stableReleaseJson("v0.6.0", draft = true),
                alphaReleaseJson("v0.6.0-alpha.1"),
                stableReleaseJson("v0.5.9", assets = emptyList()),
                stableReleaseJson("v0.5.8"),
            ).joinToString(prefix = "[", postfix = "]")
        )

        val latest = AppReleaseSource.parseLatestStable(releases)!!

        assertThat(latest.versionName).isEqualTo("0.5.8")
    }

    @Test
    fun `stable versions order numerically, not lexically`() {
        val releases = JSONArray("[${stableReleaseJson("v0.10.0")},${stableReleaseJson("v0.9.0")}]")

        assertThat(AppReleaseSource.parseLatestStable(releases)!!.versionName).isEqualTo("0.10.0")
    }

    @Test
    fun `newest alpha is picked by version even when the API lists it later`() {
        val releases = JSONArray(
            listOf(
                alphaReleaseJson("v0.3.0-alpha.9"),
                alphaReleaseJson("v0.3.0-alpha.16"),
                alphaReleaseJson("v0.3.0-alpha.15"),
            ).joinToString(prefix = "[", postfix = "]")
        )

        val latest = AppReleaseSource.parseLatestAlpha(releases, listOf("arm64-v8a"))!!

        assertThat(latest.versionName).isEqualTo("0.3.0-alpha.16")
        assertThat(latest.apkUrl).endsWith("/CarpeLines-0.3.0-alpha.16-arm64-v8a.apk")
    }

    @Test
    fun `alpha asset follows the device ABI preference order`() {
        val releases = JSONArray("[${alphaReleaseJson("v0.3.0-alpha.16")}]")

        val latest = AppReleaseSource.parseLatestAlpha(releases, listOf("armeabi-v7a", "arm64-v8a"))!!

        assertThat(latest.apkUrl).endsWith("-armeabi-v7a.apk")
    }

    @Test
    fun `drafts, stable tags and releases without a matching APK are not offered`() {
        val releases = JSONArray(
            listOf(
                alphaReleaseJson("v0.3.0-alpha.20", draft = true),
                alphaReleaseJson("v0.4.0", prerelease = false),
                alphaReleaseJson("v0.3.0-alpha.19", abis = listOf("x86_64")),
                alphaReleaseJson("v0.3.0-alpha.18"),
            ).joinToString(prefix = "[", postfix = "]")
        )

        assertThat(AppReleaseSource.parseLatestAlpha(releases, listOf("arm64-v8a"))!!.versionName)
            .isEqualTo("0.3.0-alpha.18")
        assertThat(AppReleaseSource.parseLatestAlpha(releases, listOf("x86"))).isNull()
    }

    @Test
    fun `alpha versions order by base version before build number`() {
        val olderBaseHigherBuild = alphaAppRelease("0.3.0-alpha.99")
        val newerBase = alphaAppRelease("0.4.0-alpha.1")

        assertThat(newerBase.isNewerThan("0.3.0-alpha.99", installedVersionCode = 300099)).isTrue()
        assertThat(olderBaseHigherBuild.isNewerThan("0.4.0-alpha.1", installedVersionCode = 400001)).isFalse()
        assertThat(alphaAppRelease("0.3.0-alpha.16").isNewerThan("0.3.0-alpha.16", 300016)).isFalse()
    }

    @Test
    fun `PR builds on the alpha key are offered any published alpha`() {
        assertThat(alphaAppRelease("0.3.0-alpha.16").isNewerThan("0.3.0-pr.137.ba94188", 3)).isTrue()
    }

    @Test
    fun `stable releases compare by release tag`() {
        assertThat(stableAppRelease("0.5.1").isNewerThan("0.5.0", 50000)).isTrue()
        assertThat(stableAppRelease("0.10.0").isNewerThan("0.9.9", 90099)).isTrue()
        assertThat(stableAppRelease("0.5.0").isNewerThan("0.5.0", 50000)).isFalse()
        assertThat(stableAppRelease("0.5.0").isNewerThan("0.6.0", 60000)).isFalse()
        // A CI or hand-built install has no stable tag; any release brings it back on channel.
        assertThat(stableAppRelease("0.5.0").isNewerThan("0.5.0-pr.137.ba94188", 1)).isTrue()
    }

    @Test
    fun `installed channel follows the version name published by each pipeline`() {
        assertThat(UpdateChannel.ofVersionName("0.3.0")).isEqualTo(UpdateChannel.STABLE)
        assertThat(UpdateChannel.ofVersionName("0.3.0-alpha.16")).isEqualTo(UpdateChannel.ALPHA)
        assertThat(UpdateChannel.ofVersionName("0.3.0-pr.137.ba94188")).isEqualTo(UpdateChannel.ALPHA)
    }
}
