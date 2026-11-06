# Contributing to CarpeLines

Thanks for taking the time to contribute. This document explains how the
repository is laid out, how to build it, and what a good change looks like.

## Before you start

- **Bug reports and feature requests** belong in
  [issues](https://github.com/jayfunc/CarpeLines/issues). Use the templates so the
  report includes the app version, Android version, player and lyrics source.
- **Security issues** must not be filed publicly. See [SECURITY.md](SECURITY.md).
- **Large changes** — new lyrics providers, a new screen, a database migration —
  should start as an issue so the design can be agreed on before you write code.

## Repository layout

The repository root is a plain container; the Gradle project lives one level
down in `src/CarpeLines.KMP`.

```text
src/CarpeLines.KMP/          Gradle root (rootProject.name = "CarpeLines")
- app/                      Android application module
- shared/                   Kotlin Multiplatform lyrics engine
- baselineprofile/          Macrobenchmark and baseline profile generation
- docs/                     Release, dependency and design notes
- gradle/                   Version catalog and Gradle wrapper
```

Run every Gradle task from `src/CarpeLines.KMP`.

## Requirements

| Tool | Version |
| :--- | :--- |
| JDK | 21 |
| Android SDK | compileSdk 37, minSdk 30 |
| Gradle | via the wrapper (`gradlew` / `gradlew.bat`) |

`local.properties` with `sdk.dir=...` is created by Android Studio and is not
committed.

## Build and test

```sh
cd src/CarpeLines.KMP

./gradlew :app:compileDebugKotlin      # compile the app
./gradlew :app:testDebugUnitTest       # JVM unit tests
./gradlew :shared:jvmTest              # lyrics engine tests
./gradlew :app:lintDebug               # Android lint
```

A universal debug APK (no ABI splits):

```sh
./gradlew :app:assembleDebug -Pcarpelines.enableAbiSplits=false
```

On Windows use `gradlew.bat` and quote the property:
`.\gradlew.bat :app:assembleDebug "-Pcarpelines.enableAbiSplits=false"`.

## Coding conventions

- Kotlin, official code style (4-space indent), no wildcard imports.
- Compose UI code follows the Material 3 Expressive rules documented in
  [docs/M3E-DESIGN-SYSTEM.md](docs/M3E-DESIGN-SYSTEM.md).
- Keep lyrics parsing, matching and merging in `shared/` free of Android
  dependencies; the app bridges to it through `data/carpelines/`.
- All user-facing strings go into `app/src/main/res/values/strings*.xml`; add
  translations for the locales listed under `values-*` where you can.
- Do not commit build output, `local.properties`, keystores or signing
  properties. The root `.gitignore` already covers them.

## Commit and pull requests

- Keep commits focused; one concern per commit.
- Write commit messages in the imperative mood and explain *why* the change is
  needed.
- Fill in the pull request template, including the commands you ran and their
  results.
- CI is started manually from the Actions tab (or `gh workflow run CI`); it no
  longer runs on every push or pull request. Run the same checks locally — lyrics
  engine tests, a debug build and the unit tests — and paste the results into the
  pull request.

## License

CarpeLines is licensed under GPL-3.0-or-later. By contributing you agree that
your work is released under the same license.
