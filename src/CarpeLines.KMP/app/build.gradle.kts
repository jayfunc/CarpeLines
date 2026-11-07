import com.android.build.api.variant.FilterConfiguration
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.dagger.hilt.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.baselineprofile)
    id("kotlin-parcelize")
}

val keystoreProperties = Properties().apply {
    val propFile = rootProject.file("keystore.properties")
    if (propFile.exists()) {
        propFile.inputStream().use { load(it) }
    }
}

val releaseSigningStoreFile = rootProject.file(
    keystoreProperties.getProperty("storeFile") ?: "release.jks"
)
val disableReleaseSigning = providers.gradleProperty("carpelines.disableReleaseSigning")
    .getOrElse("false")
    .toBoolean()
val hasReleaseSigningConfig = !disableReleaseSigning &&
    releaseSigningStoreFile.isFile &&
    keystoreProperties.getProperty("storePassword") != null &&
    keystoreProperties.getProperty("keyAlias") != null &&
    keystoreProperties.getProperty("keyPassword") != null

val enableAbiSplits = providers.gradleProperty("carpelines.enableAbiSplits")
    .getOrElse("true")
    .toBoolean()

val enableComposeCompilerReports = providers.gradleProperty("carpelines.enableComposeCompilerReports")
    .getOrElse("false")
    .toBoolean()

// ---------------------------------------------------------------------------
// 版本号派生
//
// 版本号唯一来源是 version.properties 的 version=<major>.<minor>.<patch>，发版只改这一行。
// 构建标识是 HEAD 提交时间（UTC，yyyyMMdd.HHmm），构建哈希是 HEAD 的 7 位短 hash。
// 频道由 -Pcarpelines.channel=alpha|beta|stable 指定，本地构建默认 dev。
//
//   versionName = <version>-<channel>.<commit count>
//                 例如 1.0.0-beta.57
//   versionCode = 基数(APP_VERSION_CODE_BASE，默认 1000) + 提交数
//
// 发布 tag 由 CI 另用提交时间（<channel>.<yyyyMMdd.HHmm>）生成，用于 GitHub Releases
// 的同日排序；APK 名和 versionName 保持简短的构建号形式，更新检查也从资产名读取它。
//
// 单次构建仍可覆盖：-PAPP_VERSION_NAME=... -PAPP_VERSION_CODE=...
// ---------------------------------------------------------------------------

val versionProperties = Properties().apply {
    val file = rootProject.file("version.properties")
    if (file.isFile) file.inputStream().use { load(it) }
}

/** 在 Gradle 根目录执行 git 命令；git 不可用或命令失败时返回 null。 */
fun gitOutput(vararg args: String): String? = runCatching {
    providers.exec {
        workingDir = rootProject.projectDir
        commandLine("git", *args)
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim().takeIf { it.isNotEmpty() }
}.getOrNull()

val appVersionNumber = versionProperties.getProperty("version")
    ?.trim()
    ?.takeIf { it.matches(Regex("""\d+\.\d+\.\d+""")) }
    ?: error("version.properties must define version=<major>.<minor>.<patch>")

val buildChannel = providers.gradleProperty("carpelines.channel")
    .getOrElse("dev")
    .lowercase()
    .also { channel ->
        require(channel in setOf("alpha", "beta", "stable", "dev")) {
            "carpelines.channel must be alpha, beta, stable or dev (was '$channel')"
        }
    }

val appVersionCodeBase = providers.gradleProperty("APP_VERSION_CODE_BASE")
    .orNull?.toIntOrNull() ?: 1000
val gitCommitCount = gitOutput("rev-list", "--count", "HEAD")?.toIntOrNull()
val gitCommitHash = gitOutput("rev-parse", "--short=7", "HEAD")
val gitCommitSha = gitOutput("rev-parse", "HEAD")
// CI 传入实际构建时刻；本地构建回退到提交时间，避免配置缓存把旧的时间戳留在 APK 里。
val buildTimeEpochSeconds = providers.gradleProperty("APP_BUILD_TIME_EPOCH")
    .orNull?.toLongOrNull()
    ?: gitOutput("show", "-s", "--format=%ct", "HEAD")?.toLongOrNull()
    ?: 0L
val gitVersionName =
    if (gitCommitCount != null) {
        "$appVersionNumber-$buildChannel.$gitCommitCount"
    } else {
        appVersionNumber
    }

val appVersionName = providers.gradleProperty("APP_VERSION_NAME")
    .orNull?.takeIf { it.isNotBlank() } ?: gitVersionName
val appVersionCode = providers.gradleProperty("APP_VERSION_CODE").orNull?.toIntOrNull()
    ?: (appVersionCodeBase + (gitCommitCount ?: 0))

@Suppress("DEPRECATION")
android {
    namespace = "io.github.jayfunc.carpelines"
    compileSdk = 37

    sourceSets {
        getByName("androidTest") {
            assets.directories.add(file("$projectDir/schemas").path)
        }
    }

    androidResources {
        // Keep only the languages we actually ship. Without this the app picks up
        // every locale our AndroidX dependencies carry, which bloats the APK and
        // makes the system language picker offer languages we have no strings for.
        localeFilters.addAll(
            listOf(
                "en", "ar", "de", "es", "fr", "in",
                "it", "ko", "nb", "ru", "tr", "zh-rCN"
            )
        )
    }

    packaging {
        resources {
            excludes += listOf(
                "META-INF/INDEX.LIST",
                "META-INF/DEPENDENCIES",
                "/META-INF/io.netty.versions.properties",
                "META-INF/CONTRIBUTORS.md",
                "META-INF/NOTICE.txt",
                "META-INF/NOTICE.md"
            )
            pickFirsts += listOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE.txt"
            )
        }
    }

    defaultConfig {
        applicationId = "io.github.jayfunc.carpelines"
        minSdk = 30
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName

        buildConfigField("String", "GIT_COMMIT_SHA", "\"${gitCommitSha.orEmpty()}\"")
        buildConfigField("String", "GIT_COMMIT_SHORT", "\"${gitCommitHash.orEmpty()}\"")
        buildConfigField("long", "BUILD_TIME_EPOCH_SECONDS", "${buildTimeEpochSeconds}L")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigningConfig) {
            create("release") {
                storeFile = releaseSigningStoreFile
                storePassword = checkNotNull(keystoreProperties.getProperty("storePassword"))
                keyAlias = checkNotNull(keystoreProperties.getProperty("keyAlias"))
                keyPassword = checkNotNull(keystoreProperties.getProperty("keyPassword"))
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }

        release {
            if (hasReleaseSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        create("benchmark") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.all { it.useJUnitPlatform() }
    }

    lint {
        checkReleaseBuilds = false
    }

    splits {
        abi {
            isEnable = enableAbiSplits
            reset()
            if (enableAbiSplits) {
                include("arm64-v8a", "armeabi-v7a")
                isUniversalApk = false
            }
        }
    }

    bundle {
        abi.enableSplit = true
        density.enableSplit = true
        language.enableSplit = true
    }
}

// 产物名带完整的发布标识，例如
//   CarpeLines-1.0.0-beta.57-universal.apk
//   CarpeLines-1.0.0-alpha.57-arm64-v8a.apk
// versionName 已包含频道、版本号和构建号；这里补上 ABI（关闭 ABI 拆分时
// 写 universal），未签名构建补 -unsigned，非 release 构建类型补构建类型名。
androidComponents {
    onVariants { variant ->
        val isSigned = variant.buildType == "debug" || hasReleaseSigningConfig
        variant.outputs.forEach { output ->
            val abi = output.filters
                .firstOrNull { it.filterType == FilterConfiguration.FilterType.ABI }
                ?.identifier
            val suffix = buildString {
                append("-").append(abi ?: "universal")
                when {
                    !isSigned -> append("-unsigned")
                    variant.buildType != "release" -> append("-").append(variant.name)
                }
                append(".apk")
            }
            output.outputFileName.set(output.versionName.map { versionName -> "CarpeLines-$versionName$suffix" })
        }
    }
}

composeCompiler {
}

baselineProfile {
    automaticGenerationDuringBuild = false
    saveInSrc = true
    dexLayoutOptimization = true
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.generateKotlin", "true")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xannotation-default-target=param-property")

        if (enableComposeCompilerReports) {
            val buildDir = project.layout.buildDirectory.get().asFile.absolutePath
            freeCompilerArgs.addAll(
                "-P", "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=$buildDir/compose_compiler_reports",
                "-P", "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=$buildDir/compose_compiler_metrics"
            )
        }

        freeCompilerArgs.addAll(
            "-P", "plugin:androidx.compose.compiler.plugins.kotlin:stabilityConfigurationPath=${project.rootDir.absolutePath}/app/compose_stability.conf"
        )
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.androidx.profileinstaller)
    "baselineProfile"(project(":baselineprofile"))
    implementation(project(":shared"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.lifecycleprocess)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.constraintlayout.compose)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.animation)
    implementation(libs.androidx.palette.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.material)
    implementation(libs.androidx.appcompat)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation.runtime.ktx)

    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.paging.common)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.exoplayer.ffmpeg)
    implementation(libs.androidx.media3.exoplayer.midi)
    implementation(libs.androidx.media3.transformer)
    implementation(libs.androidx.media)
    implementation(libs.coil.compose)
    implementation(libs.taglib)
    implementation(libs.jaudiotagger)
    implementation(libs.vorbisjava.core)
    implementation(libs.wavy.slider)
    implementation(libs.androidx.graphics.shapes)

    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    implementation(libs.gson)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.snakeyaml)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.security.crypto)

    implementation(libs.timber)
    implementation(libs.smooth.corner.rect.android.compose)
    implementation(libs.reorderables)
    implementation(libs.androidx.glance)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.kuromoji.ipadic)
    implementation(libs.pinyin4j.core)
    implementation(libs.accompanist.drawablepainter)
    implementation(libs.accompanist.permissions)
    implementation(libs.accompanist.lyrics.core)
    implementation(libs.accompanist.lyrics.ui)
    implementation(libs.capturable) {
        exclude(group = "androidx.compose.animation")
        exclude(group = "androidx.compose.foundation")
        exclude(group = "androidx.compose.material")
        exclude(group = "androidx.compose.runtime")
        exclude(group = "androidx.compose.ui")
    }

    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit)
    testRuntimeOnly(libs.junit.vintage.engine)
    testRuntimeOnly(libs.junitplatformlauncher)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
    testImplementation(libs.org.json)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.junit)
    testImplementation(kotlin("test"))

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.worktesting)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.uiautomator)

    debugImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    constraints {
        implementation(libs.netty.common)
        implementation(libs.netty.handler)
        implementation(libs.netty.codec.http)
        implementation(libs.netty.codec.http2)
        implementation(libs.bouncycastle.bcprov)
        implementation(libs.bouncycastle.bcpkix)
        implementation(libs.commons.lang3)
        implementation(libs.jdom2)
        implementation(libs.jose4j)
        implementation(libs.apache.httpclient)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
