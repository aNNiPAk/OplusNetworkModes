# Oplus Network Modes

[Русский](README.ru.md)

Adds network modes to stock ColorOS/OxygenOS settings and shows the exact
selected mode on the SIM page. English and Russian labels included.

Modes: 5G/4G/3G/2G, 5G/4G/3G, 5G/4G, 5G only, 4G/3G/2G, 4G/3G,
4G only, 3G/2G, 3G only and 2G only.

## Install

1. Install the APK from [Releases](https://github.com/aNNiPAk/OplusNetworkModes/releases).
2. Enable the module in Vector/LSPosed with **modern API 102** support.
3. Select **Phone Services (`com.android.phone`)** and reboot.

Root required. No launcher icon or separate settings.
DexKit finds firmware methods automatically and caches the results.
Tested on OnePlus CPH2767, OxygenOS 16 and 17; other firmware may differ.
LTE-only calls require working VoLTE.

## Build

JDK 17+, Android SDK 36.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

On Windows use `gradlew.bat`. APK: `app/build/outputs/apk/debug/app-debug.apk`.
GitHub Actions builds each push and pull request. [Release signing](docs/RELEASING.md).

## License

[MIT](LICENSE) · [Third-party notices](THIRD_PARTY_NOTICES.md)
