## What changed

<!-- One or two sentences on the user-visible effect of this change. -->

## Why

<!-- The problem this solves. Link an issue when there is one: Closes #123 -->

## How it was verified

<!-- Delete what does not apply, add results. -->

- [ ] `./gradlew :shared:jvmTest`
- [ ] `./gradlew :app:testDebugUnitTest`
- [ ] `./gradlew :app:compileDebugKotlin`
- [ ] `./gradlew :app:lintDebug`
- [ ] Installed on a device and checked the affected screen

## Notes for reviewers

<!-- Screenshots for UI changes, migration notes, follow-up work. -->

## Checklist

- [ ] The change is focused and the commits explain why.
- [ ] New or changed user-facing strings are in `strings.xml` (all `values-*` locales updated where possible).
- [ ] No build output, `local.properties`, keystores or signing properties are included.
- [ ] I agree to license this contribution under GPL-3.0-or-later.
