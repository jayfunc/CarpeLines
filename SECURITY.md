# Security Policy

## Supported versions

Only the latest release on `main` receives security fixes. Older builds are
not supported.

| Version | Supported |
| :--- | :--- |
| Latest release | Yes |
| Any earlier build | No |

## Reporting a vulnerability

Please **do not** open a public issue for a security problem.

Report privately through GitHub's
[private vulnerability reporting](https://github.com/jayfunc/CarpeLines/security/advisories/new).
If that is unavailable, contact the maintainer listed in
[README.md](README.md#maintainer) directly.

Include:

- the affected version (the version shown in Settings, which matches the `v<X.Y.Z>` release tag),
- the Android version and device,
- a description of the issue and its impact,
- reproduction steps or a proof of concept.

You can expect an acknowledgement within a few days and a fix or mitigation
plan once the report has been triaged. Please allow time for a release before
disclosing the issue publicly.

## Scope

CarpeLines reads the system media session and lyrics from third-party services.
Reports about the app's own handling of permissions, notifications, stored
lyrics, exported files and network requests are in scope. Vulnerabilities in
third-party players or lyrics providers are out of scope and should be reported
to those projects.
