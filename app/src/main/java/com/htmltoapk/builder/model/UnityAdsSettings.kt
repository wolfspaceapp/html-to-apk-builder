package com.htmltoapk.builder.model

import kotlinx.serialization.Serializable

@Serializable
data class UnityAdsSettings(
    val enabled: Boolean = false,
    val gameId: String = "", // Unity Game ID
    val testMode: Boolean = true,

    // Banner
    val bannerEnabled: Boolean = false,
    val bannerPlacementId: String = "banner",
    val bannerPosition: String = "bottom", // top, bottom

    // Interstitial
    val interstitialEnabled: Boolean = false,
    val interstitialPlacementId: String = "interstitial",
    val interstitialIntervalSeconds: Int = 60,
    val interstitialPageCount: Int = 3,

    // Rewarded
    val rewardedEnabled: Boolean = false,
    val rewardedPlacementId: String = "rewardedVideo",

    // Auto-load
    val autoLoadInterstitial: Boolean = true,
    val autoLoadRewarded: Boolean = true,

    // GDPR/Privacy
    val consentRequired: Boolean = false,
    val metadata: String = "" // JSON for additional config
)