package com.htmltoapk.builder.model

import kotlinx.serialization.Serializable

@Serializable
data class BillingSettings(
    val enabled: Boolean = false,
    val type: String = "subscription", // subscription, inapp
    val productId: String = "",
    val productName: String = "",
    val productDescription: String = "",
    val subscriptionId: String = "",
    val subscriptionPeriod: String = "P1M", // ISO 8601 duration
    val freeTrialDays: Int = 0,
    val introductoryPrice: String = "",
    val acknowledge: Boolean = true,
    val autoConsume: Boolean = true,
    val enablePendingPurchases: Boolean = true,
    val testMode: Boolean = true
)