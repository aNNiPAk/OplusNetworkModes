package dev.networkmodes.oplus

internal data class FirmwareProfile(
    val name: String,
    val activityClass: String,
    val platformAdapterClass: String,
    val adapterClass: String,
    val listField: String,
    val slotField: String,
    val currentModeMethod: String,
    val selectedIndexMethod: String,
    val summaryMethod: String,
    val summaryModeMethod: String,
    val labelsField: String = "i",
    val modesField: String = "j",
    val indexField: String = "k",
    val adapterField: String = "m",
)
