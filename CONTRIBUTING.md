# Contributing

Use Kotlin, JVM target 17, UTF-8 and four-space indentation. Before submitting:

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Keep mode values and translations in `NetworkMode`.
For hook changes, check the list, selected entry and SIM summary on a device,
including after a reboot. Unit tests do not verify OEM hooks.

Report the device, firmware, framework version and relevant
`OplusNetworkModes` logs. Exclude personal data, firmware APKs and signing keys.
