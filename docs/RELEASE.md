# Release Checklist

CarpeLines releases are shipped from `main` after the release candidate passes local checks and a basic device smoke test.

Paths in this document are relative to the Gradle root (`src/CarpeLines.KMP`) unless a command says otherwise.

## Versioning

`versionName` and `versionCode` are derived from git by `app/build.gradle.kts`, so a release
never edits a version file:

- `versionName` is the nearest tag with its leading `v` stripped (`v0.1.0` → `0.1.0`,
  `v0.2.0-alpha.3` → `0.2.0-alpha.3`). Builds after that tag get `-dev.<commits>-g<hash>`
  appended, which keeps them out of the stable channel.
- `versionCode` is `1000` (`APP_VERSION_CODE_BASE`) plus the number of commits reachable
  from `HEAD`. It only ever grows, so upgrades install in place.

Both values can be pinned for a single build when needed:

```sh
./gradlew :app:assembleRelease -PAPP_VERSION_NAME=0.1.0 -PAPP_VERSION_CODE=1001
```

Derivation needs the full git history and tags: a shallow clone reports a truncated commit
count, so build from a full clone (CI checks out with `fetch-depth: 0`) and run
`git fetch --tags` locally after someone else pushes a tag.

For every public release:

1. Confirm the working tree is clean and `main` is up to date.
2. Tag the release as `v<X.Y.Z>`, for example `v0.1.0`. Nothing else needs editing.

## Required Local Checks

Run these before tagging:

```sh
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:compileDebugKotlin
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:lintDebug
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:testDebugUnitTest
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:assembleRelease -Pcarpelines.enableAbiSplits=false -Pcarpelines.disableReleaseSigning=true
```

For split APK artifacts, use:

```sh
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:assembleRelease -Pcarpelines.enableAbiSplits=true
```

APK artifacts are named from `versionName`: `CarpeLines-<versionName>-<abi>.apk` for signed release builds, with `-unsigned` appended when no release signing config is present.

## Signing

Release signing is driven by an untracked `keystore.properties` file at the Gradle root (`src/CarpeLines.KMP`).

Supported keys:

```properties
storeFile=/absolute/or/repo-relative/path/to/release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

`storeFile` is optional when the release keystore is available as `release.jks` at the Gradle root. `storePassword`, `keyAlias`, and `keyPassword` are required for signing. If signing properties or the keystore file are missing, release builds are unsigned.

For unsigned verification builds, pass `-Pcarpelines.disableReleaseSigning=true` even when local signing files exist.

## Device Smoke Test

Install the release candidate and verify:

1. First launch and setup complete.
2. Notification access granted; the Home hero follows an external player.
3. Lyrics resolve from embedded tags, a sibling `.lrc`, or an online provider.
4. Word timing follows the active system media session.
5. Transport controls in the lyrics chrome drive the external player.
6. Lyrics settings survive an app restart.
7. Backup export flow creates a file.

## In-App Updates

The Updates screen (`data/update/`) reads the GitHub releases feed, so keep tag and asset formats stable:

- **Stable:** tagged, non-prerelease releases named `v<X.Y.Z>`. The updater prefers a universal APK asset and falls back to any architecture-specific APK the release ships. Any installed version name without a `-` suffix counts as a stable install.
- **Alpha:** GitHub prereleases tagged `v<X.Y.Z>-alpha.<N>` with assets named `CarpeLines-<X.Y.Z>-alpha.<N>-<abi>.apk`. Renaming tags or assets breaks alpha update discovery.

Stable and alpha builds are signed with different keys. The updater installs only same-channel releases whose signer matches the installed app, and walks users through uninstalling to switch channels.

## Publishing

1. Ensure `main` is clean and pushed.
2. Create a tag: `git tag v<X.Y.Z>`.
3. Push the tag: `git push origin v<X.Y.Z>`.
4. Create a GitHub release from the tag.
5. Attach APK artifacts and paste the changelog section.
6. Check the README provider list still matches the optional network services present in the app.
7. Create source archives from git, not from the working tree, so ignored local artifacts are excluded.
