# Oplus Network Modes

[English](README.md)

Добавляет режимы сети в штатные настройки ColorOS/OxygenOS и показывает
точный выбранный режим на странице SIM. Поддерживает русский и английский языки.

Режимы: 5G/4G/3G/2G, 5G/4G/3G, 5G/4G, только 5G, 4G/3G/2G, 4G/3G,
только 4G, 3G/2G, только 3G и только 2G.

## Установка

1. Установите APK из [Releases](https://github.com/aNNiPAk/OplusNetworkModes/releases).
2. Включите модуль в Vector/LSPosed с поддержкой **modern API 102**.
3. Выберите **Phone Services (`com.android.phone`)** и перезагрузитесь.

Нужен root. Отдельных настроек и значка запуска нет.
DexKit автоматически находит методы прошивки и кэширует результат.
Проверено на OnePlus CPH2767 с OxygenOS 16 и 17; на других прошивках работа может отличаться.
Для звонков в режиме «Только 4G» нужен работающий VoLTE.

## Сборка

JDK 17+, Android SDK 36.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

В Windows используйте `gradlew.bat`. APK: `app/build/outputs/apk/debug/app-debug.apk`.
GitHub Actions собирает каждый push и pull request. [Подпись релиза](docs/RELEASING.md).

## Лицензия

[MIT](LICENSE) · [Сторонние компоненты](THIRD_PARTY_NOTICES.md)
