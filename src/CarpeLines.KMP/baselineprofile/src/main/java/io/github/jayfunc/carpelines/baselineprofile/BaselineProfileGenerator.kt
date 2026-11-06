package io.github.jayfunc.carpelines.baselineprofile

import android.util.Log
import android.view.KeyEvent
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generateStartupProfile() {
        val packageName = benchmarkTargetPackageName()

        rule.collect(
            packageName = packageName,
            includeInStartupProfile = true,
            maxIterations = 5,
            stableIterations = 3
        ) {
            try {
                setupBenchmarkPermissions(packageName)
                runStep("Startup", requireAppBefore = false) { startBenchmarkActivity(packageName) }

                Log.d(TAG, "--- STARTUP FLOW FINISHED ---")
            } catch (e: Exception) {
                Log.e(TAG, "Fatal error: ${e}")
                e.printStackTrace()
                throw e
            } finally {
                pressHome()
            }
        }
    }

    @Test
    fun generateBaselineProfile() {
        val packageName = benchmarkTargetPackageName()

        rule.collect(
            packageName = packageName,
            includeInStartupProfile = false,
            maxIterations = 5,
            stableIterations = 3
        ) {
            try {
                setupBenchmarkPermissions(packageName)
                runStep("Startup", requireAppBefore = false) { startBenchmarkActivity(packageName) }
                runStep("Home and Lyrics") { runHomeFlow() }
                runStep("Settings Surfaces") { runSettingsSurfacesFlow() }

                Log.d(TAG, "--- FLOW FINISHED ---")
            } catch (e: Exception) {
                Log.e(TAG, "Fatal error: ${e}")
                e.printStackTrace()
                throw e
            } finally {
                pressHome()
            }
        }
    }

    private fun MacrobenchmarkScope.runStep(
        name: String,
        requireAppBefore: Boolean = true,
        block: MacrobenchmarkScope.() -> Unit
    ) {
        try {
            Log.d(TAG, ">> STEP: $name")
            if (requireAppBefore) {
                assertAppForeground("Before Baseline Profile step '$name'")
            }
            block()
            assertAppForeground("After Baseline Profile step '$name'")
            Log.d(TAG, ">> OK")
        } catch (e: Exception) {
            Log.e(TAG, ">> FAILED $name: ${e}")
            e.printStackTrace()
            throw IllegalStateException("Baseline Profile step failed: $name", e)
        }
    }

    private fun MacrobenchmarkScope.startBenchmarkActivity(packageName: String) {
        pressHome()
        killProcess()
        startActivityAndWait { intent ->
            intent.putExtra(BENCHMARK_EXTRA, true)
        }
        waitForTargetPackageVisible(packageName, APP_START_TIMEOUT_MS)
        waitForAppForeground("After starting benchmark activity", packageName, APP_START_TIMEOUT_MS)
        waitForUi()
        handlePermissionDialogs()
        handleOnboarding()
    }

    private fun MacrobenchmarkScope.runHomeFlow() {
        clickTab("Home|Inicio")
        waitForUi()
        scrollToListBottom(repetitions = 3)
        openOptionalSurface("Stats|Estad[ií]sticas") {
            scrollDownAndUp()
        }
        clickTab("Lyrics|Letras")
        waitForUi()
        scrollDownAndUp()
        clickTab("Home|Inicio")
        scrollToTop(repetitions = 3)
    }

    private fun MacrobenchmarkScope.runSettingsSurfacesFlow() {
        val settingsDestinations = listOf(
            "Appearance|Apariencia",
            "Lyrics|Letras",
            "Local Music|M[uú]sica local|Biblioteca|Library",
            "Backup|Restore|Copia|Restaurar",
            "Updates|Actualizaciones",
            "About|Acerca"
        )

        if (!openSettingsFromHome()) return
        settingsDestinations.forEach { label ->
            if (!ensureSettingsScreen()) return@forEach
            if (!clickOptional(pattern(label), timeoutMs = SHORT_WAIT_MS)) {
                scrollToListBottom(repetitions = 1)
                clickOptional(pattern(label), timeoutMs = SHORT_WAIT_MS)
            }
            waitForUi()
            if (!isSettingsScreen()) {
                scrollDownAndUp()
                pressBackAndWait()
            }
        }
        pressBackAndWait()
    }

    private fun MacrobenchmarkScope.handlePermissionDialogs() {
        val allowPattern = pattern("Allow", "Permitir", "Aceptar", "While using", "Mientras")
        var attemptsRemaining = 5
        while (attemptsRemaining > 0) {
            attemptsRemaining--
            val button = findPermissionAllowButton(allowPattern) ?: break
            clickSystemDialog(button)
            waitForUi(TINY_WAIT_MS, requireApp = false)
        }
        waitForAppForeground("After permission dialogs")
    }

    private fun MacrobenchmarkScope.handleOnboarding() {
        val continuePattern = pattern("Next", "Continue", "Skip", "Done", "Siguiente", "Omitir", "Empezar", "Finish")
        repeat(4) {
            val button = findByTextOrDescription(continuePattern, TINY_WAIT_MS) ?: return
            click(button)
            waitForUi()
        }
    }

    private fun MacrobenchmarkScope.clickTab(tabNamePattern: String) {
        val tab = findByTextOrDescription(pattern(tabNamePattern), SHORT_WAIT_MS)
        if (tab != null) {
            click(tab)
            waitForUi()
            return
        }

        when {
            tabNamePattern.contains("Home", ignoreCase = true) ->
                tap((device.displayWidth * 0.18).toInt(), (device.displayHeight * 0.94).toInt(), "open Home tab fallback")
            tabNamePattern.contains("Lyrics", ignoreCase = true) ->
                tap((device.displayWidth * 0.38).toInt(), (device.displayHeight * 0.94).toInt(), "open Lyrics tab fallback")
            tabNamePattern.contains("Stats", ignoreCase = true) ->
                tap((device.displayWidth * 0.62).toInt(), (device.displayHeight * 0.94).toInt(), "open Stats tab fallback")
            tabNamePattern.contains("Settings", ignoreCase = true) ->
                tap((device.displayWidth * 0.85).toInt(), (device.displayHeight * 0.94).toInt(), "open Settings tab fallback")
        }
        waitForUi()
    }

    private fun MacrobenchmarkScope.openOptionalSurface(labelPattern: String, body: MacrobenchmarkScope.() -> Unit) {
        if (!clickOptional(pattern(labelPattern), timeoutMs = SHORT_WAIT_MS)) {
            Log.w(TAG, "Optional surface not found: $labelPattern")
            return
        }
        waitForUi(EXTRA_UI_WAIT_MS)
        val openedBackStackSurface = hasTextOrDescription(pattern(BACK_NAV_ALTERNATIVES))
        body()
        if (openedBackStackSurface || hasTextOrDescription(pattern(BACK_NAV_ALTERNATIVES))) {
            pressBackAndWait()
        } else {
            Log.d(TAG, "Optional surface '$labelPattern' did not navigate; staying on current app surface.")
        }
    }

    private fun MacrobenchmarkScope.openSettingsFromHome(): Boolean {
        clickTab("Home|Inicio")
        val opened = clickOptional(pattern("Settings|Configuraci[oó]n|Ajustes"), timeoutMs = SHORT_WAIT_MS)
        if (opened) waitForUi()
        return opened
    }

    private fun MacrobenchmarkScope.ensureSettingsScreen(): Boolean {
        if (isSettingsScreen()) return true
        return openSettingsFromHome() && isSettingsScreen()
    }

    private fun MacrobenchmarkScope.isSettingsScreen(): Boolean =
        hasTextOrDescription(pattern("Settings|Configuraci[oó]n|Ajustes"))

    private fun MacrobenchmarkScope.scrollDownAndUp() {
        val midX = device.displayWidth / 2
        val bottomY = (device.displayHeight * 0.75).toInt()
        val topY = (device.displayHeight * 0.25).toInt()
        swipe(midX, bottomY, midX, topY, 45, "scroll down")
        waitForUi()
        swipe(midX, topY, midX, bottomY, 45, "scroll up")
        waitForUi()
    }

    private fun MacrobenchmarkScope.scrollToListBottom(repetitions: Int = 4) {
        val midX = device.displayWidth / 2
        val bottomY = (device.displayHeight * 0.75).toInt()
        val topY = (device.displayHeight * 0.25).toInt()
        repeat(repetitions) {
            swipe(midX, bottomY, midX, topY, 45, "scroll list toward bottom")
            waitForUi(TINY_WAIT_MS)
        }
    }

    private fun MacrobenchmarkScope.scrollToTop(repetitions: Int = 4) {
        val midX = device.displayWidth / 2
        val bottomY = (device.displayHeight * 0.75).toInt()
        val topY = (device.displayHeight * 0.25).toInt()
        repeat(repetitions) {
            swipe(midX, topY, midX, bottomY, 45, "scroll list toward top")
            waitForUi(TINY_WAIT_MS)
        }
    }

    private fun MacrobenchmarkScope.clickOptional(pattern: Pattern, timeoutMs: Long): Boolean {
        val target = findByTextOrDescription(pattern, timeoutMs) ?: return false
        click(target)
        return true
    }

    private fun MacrobenchmarkScope.findByTextOrDescription(pattern: Pattern, timeoutMs: Long): UiObject2? {
        assertAppForeground("Before finding UI object '$pattern'")
        val target = device.wait(Until.findObject(By.desc(pattern)), timeoutMs)
            ?: device.wait(Until.findObject(By.text(pattern)), timeoutMs)
        assertAppForeground("After finding UI object '$pattern'")
        return target
    }

    private fun MacrobenchmarkScope.findByDescription(pattern: Pattern, timeoutMs: Long): UiObject2? {
        assertAppForeground("Before finding description '$pattern'")
        val target = device.wait(Until.findObject(By.desc(pattern)), timeoutMs)
        assertAppForeground("After finding description '$pattern'")
        return target
    }

    private fun MacrobenchmarkScope.hasTextOrDescription(pattern: Pattern): Boolean {
        assertAppForeground("Before checking UI object '$pattern'")
        return device.hasObject(By.desc(pattern)) || device.hasObject(By.text(pattern))
    }

    private fun MacrobenchmarkScope.click(target: UiObject2) {
        assertAppForeground("Before clicking '${target.text ?: target.contentDescription ?: target.resourceName.orEmpty()}'")
        val objectPackage = target.applicationPackage.orEmpty()
        if (objectPackage.isNotBlank() && objectPackage != benchmarkTargetPackageName()) {
            throw IllegalStateException(
                "Refusing to click object from package $objectPackage while profiling ${benchmarkTargetPackageName()}"
            )
        }
        val center = target.visibleCenter
        tap(center.x, center.y, "click app UI object")
    }

    private fun MacrobenchmarkScope.tap(x: Int, y: Int, context: String) {
        assertAppForeground("Before $context")
        device.click(x, y)
        assertAppForeground("After $context")
    }

    private fun MacrobenchmarkScope.swipe(
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        steps: Int,
        context: String
    ) {
        assertAppForeground("Before $context")
        device.swipe(startX, startY, endX, endY, steps)
        assertAppForeground("After $context")
    }

    private fun MacrobenchmarkScope.pressBackAndWait() {
        assertAppForeground("Before pressing back")
        device.pressBack()
        waitForUi(requireApp = false)
        if (device.hasObject(By.pkg(benchmarkTargetPackageName()))) {
            return
        }

        Log.w(TAG, "Back left the target app; relaunching benchmark activity.")
        startActivityAndWait { intent ->
            intent.putExtra(BENCHMARK_EXTRA, true)
        }
        waitForTargetPackageVisible(benchmarkTargetPackageName(), APP_START_TIMEOUT_MS)
        waitForAppForeground("After relaunching from Back fallback")
        waitForUi()
    }

    private fun MacrobenchmarkScope.waitForUi(delayMs: Long = DEFAULT_UI_WAIT_MS, requireApp: Boolean = true) {
        device.waitForIdle(IDLE_WAIT_MS)
        Thread.sleep(delayMs)
        if (requireApp) {
            assertAppForeground("After waiting for app UI")
        }
    }

    private fun MacrobenchmarkScope.findPermissionAllowButton(pattern: Pattern): UiObject2? =
        device.findObject(By.res("com.android.permissioncontroller:id/permission_allow_button"))
            ?: device.findObject(By.res("com.android.permissioncontroller:id/permission_allow_foreground_only_button"))
            ?: device.findObject(By.res("com.google.android.permissioncontroller:id/permission_allow_button"))
            ?: device.findObject(By.res("com.google.android.permissioncontroller:id/permission_allow_foreground_only_button"))
            ?: device.wait(Until.findObject(By.text(pattern)), TINY_WAIT_MS)
            ?: device.wait(Until.findObject(By.desc(pattern)), TINY_WAIT_MS)

    private fun MacrobenchmarkScope.clickSystemDialog(target: UiObject2) {
        val center = target.visibleCenter
        device.click(center.x, center.y)
    }

    private fun pattern(vararg alternatives: String): Pattern =
        pattern(alternatives.joinToString("|"))

    private fun pattern(alternatives: String): Pattern =
        Pattern.compile(".*($alternatives).*", Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE)

    private companion object {
        private const val TAG = "BaselineProfileGenerator"
        private const val BACK_NAV_ALTERNATIVES = "Back|Navigate up|Atr[aá]s|Volver"
        private const val APP_START_TIMEOUT_MS = 15_000L
        private const val IDLE_WAIT_MS = 3_000L
        private const val TINY_WAIT_MS = 500L
        private const val SHORT_WAIT_MS = 1_500L
        private const val DEFAULT_UI_WAIT_MS = 900L
        private const val EXTRA_UI_WAIT_MS = 1_800L
        private const val LIBRARY_SYNC_MIN_WAIT_MS = 12_000L
    }
}
