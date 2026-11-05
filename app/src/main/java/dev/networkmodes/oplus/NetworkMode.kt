package dev.networkmodes.oplus

import java.util.Locale

internal enum class NetworkMode(
    private val value: Int,
    private val englishLabel: String,
    private val russianLabel: String = englishLabel,
) {
    // Mode 33 also includes legacy TD-SCDMA/CDMA/EVDO technologies.
    AUTO_5G(33, "Auto: 5G/4G/3G/2G", "Авто: 5G/4G/3G/2G"),
    NR_LTE_WCDMA(28, "5G/4G/3G"),
    NR_LTE(24, "5G/4G"),
    NR_ONLY(23, "5G only", "Только 5G"),
    AUTO_4G(9, "Auto: 4G/3G/2G", "Авто: 4G/3G/2G"),
    LTE_WCDMA(12, "4G/3G"),
    LTE_ONLY(11, "4G only", "Только 4G"),
    AUTO_3G(0, "Auto: 3G/2G", "Авто: 3G/2G"),
    WCDMA_ONLY(2, "3G only", "Только 3G"),
    GSM_ONLY(1, "2G only", "Только 2G");

    private fun label(locale: Locale) = if (locale.language == "ru") russianLabel else englishLabel

    companion object {
        fun labels(locale: Locale): Array<String> = Array(entries.size) { entries[it].label(locale) }

        // The OEM expects Integer[], not a primitive int array.
        fun networkTypes(): Array<Int> = Array(entries.size) { entries[it].value }

        /** Null preserves the firmware's original summary for unknown modes. */
        fun labelForValue(value: Int, locale: Locale): String? =
            entries.firstOrNull { it.value == value }?.label(locale)
    }
}
