# Releases

GitHub Actions tests and builds every push and pull request.
Debug APKs are available in the workflow artifacts.

For signed releases, configure these repository secrets:

| Secret | Value |
| --- | --- |
| `SIGNING_KEYSTORE_BASE64` | Base64-encoded keystore |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | Signing alias |
| `ANDROID_KEY_PASSWORD` | Key password |

Keep the same signing key for updates. Set the version in `gradle.properties`,
then push a matching tag, for example `v1`. The workflow publishes the signed
APK and SHA-256 checksum. Existing releases are not overwritten.

For a local signed build, set `ANDROID_KEYSTORE_PATH`, `ANDROID_KEYSTORE_PASSWORD`,
`ANDROID_KEY_ALIAS` and `ANDROID_KEY_PASSWORD`, then run `./gradlew assembleRelease`.

Version 1 uses version code 1. Android treats it as a downgrade from earlier
version 2 builds; a normal in-place update from those builds is not supported.
