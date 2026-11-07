# Release Checklist

CarpeLines builds are produced and published by the manually triggered CI workflow
(`.github/workflows/ci.yml`). Every run builds one signed APK, generates release notes from git,
and creates (or updates) a GitHub Release for the selected channel.

Paths in this document are relative to the Gradle root (`src/CarpeLines.KMP`) unless a command
says otherwise.

## Versioning

`version.properties` in the Gradle root is the single source of the version number. A release
only edits that one line:

```properties
version=1.0.0
```

Gradle derives everything else from the checkout:

- **Build number** — `git rev-list --count HEAD`.
- **Release stamp** — the `HEAD` commit time in UTC, formatted `yyyyMMdd.HHmm`, for example
  `20261106.1357`. It is used only in the release tag.
- **Commit hash** — the first 7 characters of `HEAD`.
- **`versionName`** — `<version>-<channel>.<build>`, for example `1.0.0-beta.57`.
- **`versionCode`** — `1000` (`APP_VERSION_CODE_BASE`) plus the build number. It only grows with
  the commit count, so same-channel upgrades install in place.

The tag uses the stamp because GitHub Releases sorts same-day releases by comparing tag strings,
not numbers: a variable-length build number would put `alpha.9` above `alpha.12`. The APK name and
`versionName` keep the short build number, and the in-app updater reads the released version from
the APK asset name.

The channel comes from `-Pcarpelines.channel=alpha|beta|stable`. Local builds default to `dev`.
Both values can still be pinned for a single build:

```sh
./gradlew :app:assembleRelease -Pcarpelines.channel=alpha -PAPP_VERSION_NAME=0.1.0 -PAPP_VERSION_CODE=1001
```

Derivation needs the full git history: a shallow clone reports a truncated build number, so CI
checks out with `fetch-depth: 0`.

## Channels

| Channel | GitHub release | Tag | APK asset |
| --- | --- | --- | --- |
| `alpha` | prerelease | `v1.0.0-alpha.20261106.1357` | `CarpeLines-1.0.0-alpha.57-universal.apk` |
| `beta` | prerelease | `v1.0.0-beta.20261106.1357` | `CarpeLines-1.0.0-beta.57-universal.apk` |
| `stable` | latest release | `v1.0.0-stable.20261106.1357` | `CarpeLines-1.0.0-stable.57-universal.apk` |

The release title is `CarpeLines <version> <Channel> <build>`, for example
`CarpeLines 1.0.0 Alpha 15`. It is cosmetic: GitHub orders releases by the tag (created date, then
the tag's pre-release string), so the title never affects sorting.

The tag carries version, channel and commit-time stamp; the APK asset and `versionName` carry the
build number instead, and assets also name the ABI. Running the workflow again for the same
commit produces the same stamp, so it updates the existing release instead of creating a second
one. ABI-split builds replace `-universal` with the ABI name. The release body repeats the tag,
build number and commit link.

## Required Local Checks

Run these before triggering CI:

```sh
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:compileDebugKotlin
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:lintDebug
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:testDebugUnitTest
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:assembleRelease -Pcarpelines.channel=stable -Pcarpelines.enableAbiSplits=false -Pcarpelines.disableReleaseSigning=true
```

## Signing

CI restores the release keystore from repository secrets before building:

| Secret | Contents |
| --- | --- |
| `KEYSTORE_BASE64` | Base64 of the `.jks` file |
| `KEYSTORE_STORE_PASSWORD` | Keystore password |
| `KEYSTORE_KEY_ALIAS` | Key alias |
| `KEYSTORE_KEY_PASSWORD` | Key password |

The workflow writes `keystore.properties` at the Gradle root and passes no signing overrides, so
`app/build.gradle.kts` signs the release. All three channels use this one key; switching channels
still needs the reinstall flow because the updater only installs within the selected channel.

Local signing keeps working through an untracked `keystore.properties`:

```properties
storeFile=/absolute/or/repo-relative/path/to/release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

`storeFile` is optional when the release keystore is available as `release.jks` at the Gradle
root. For unsigned verification builds, pass `-Pcarpelines.disableReleaseSigning=true` even when
local signing files exist.

## Device Smoke Test

Install the release candidate and verify:

1. First launch and setup complete.
2. Notification access granted; the Home hero follows an external player.
3. Lyrics resolve from embedded tags, a sibling `.lrc`, or an online provider.
4. Word timing follows the active system media session.
5. Transport controls in the lyrics chrome drive the external player.
6. Lyrics settings survive an app restart.
7. Backup export flow creates a file.

## Publishing

1. Ensure `main` is clean, pushed, and has the intended `version.properties`.
2. Open **Actions → CI → Run workflow** and choose `alpha`, `beta` or `stable`.
3. CI runs the lyrics and app unit tests, assembles a signed universal release APK, generates
   the changelog, and publishes the GitHub Release with the APK attached directly (no zip).

The workflow is manual only. Re-running it on the same commit updates the existing release and
overwrites its asset instead of failing on a duplicate tag.

## Changelog

`.github/scripts/generate-changelog.sh` builds the notes from
[Conventional Commits](https://www.conventionalcommits.org/) since the previous release tag of
the same channel:

- `feat` → Features, `fix` → Fixes, `perf` → Performance, `refactor` → Refactoring,
  `docs`, `build`, `ci`, `test`, `revert`, `chore` each get their own section.
- A scope is kept: `fix(lyrics): fade lyrics` becomes `**lyrics:** fade lyrics (hash)`.
- Commits that do not follow the convention land under Other changes; merge commits are skipped.
- The first release of a channel falls back to the newest tag of any channel, then to the whole
  history.

So commit messages should follow `type(scope): summary` for readable release notes.

## In-App Updates

The Updates screen (`data/update/`) reads the GitHub releases feed and discovers one channel at a
time:

- Tags supply the channel and GitHub's ordering; they are parsed as
  `<version>[-<channel>.<stamp>][-<hash>]`, where `<stamp>` is the commit time (`yyyyMMdd.HHmm`)
  or, for older releases, the commit count. Legacy tags `v0.5.1` and `v0.3.0-alpha.16` still
  resolve to their channel.
- The released version, used for display and comparison, comes from the asset name
  `CarpeLines-<versionName>-<abi>.apk`, which carries the build number (`1.0.0-alpha.15`).
- Stable releases are non-prereleases; alpha and beta are prereleases distinguished by the tag.
- Asset lookup prefers the device ABI, then the `-universal` APK.

Because the tag scheme changed, builds installed from commit-count releases (up to
`v1.0.0-alpha.12`) cannot parse commit-time stamps; users need one manual install of the first
stamped release to get back onto automatic updates.
