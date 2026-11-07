package com.htmltoapk.builder.model

import kotlinx.serialization.Serializable

@Serializable
data class AdMobSettings(
    val enabled: Boolean = false,
    val appId: String = "",
    val bannerEnabled: Boolean = false,
    val bannerUnitId: String = "",
    val bannerPosition: String = "bottom", // top, bottom
    val showBannerOnStart: Boolean = true,
    val interstitialEnabled: Boolean = false,
    val interstitialUnitId: String = "",
    val interstitialIntervalSeconds: Int = 60,
    val interstitialPageCount: Int = 3,
    val rewardedEnabled: Boolean = false,
    val rewardedUnitId: String = "",
    val autoLoadInterstitial: Boolean = true,
    val autoLoadRewarded: Boolean = true,
    val useTestAds: Boolean = true,
    val testDeviceIds: String = "",
    val maxAdContentRating: String = "G", // G, PG, T, MA
    val childDirectedTreatment: Int = 0, // 0 = unspecified, 1 = true, 2 = false
    val underAgeOfConsent: Int = 0 // 0 = unspecified, 1 = true, 2 = false
)