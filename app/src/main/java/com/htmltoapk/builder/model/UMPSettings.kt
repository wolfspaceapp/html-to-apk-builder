package com.htmltoapk.builder.model

import kotlinx.serialization.Serializable

@Serializable
data class UMPSettings(
    val debugEnabled: Boolean = false,
    val debugGeography: String = "", // EEA, NOT_EEA, DISABLED
    val testDeviceIds: String = ""
)