package com.htmltoapk.builder.model

import kotlinx.serialization.Serializable

@Serializable
data class StartAppSettings(
    val enabled: Boolean = false,
    val appId: String = "", // StartApp App ID
    val devId: String = "", // StartApp Developer ID
    val testMode: Boolean = true,

    // Banner
    val bannerEnabled: Boolean = false,
    val bannerType: String = "STANDARD", // STANDARD, AUTO, RECENT, RECENT_STANDARD, THREE_D, SPLASH
    val bannerPosition: String = "bottom", // top, bottom

    // Interstitial
    val interstitialEnabled: Boolean = false,
    val interstitialIntervalSeconds: Int = 60,
    val interstitialPageCount: Int = 3,

    // Rewarded Video (StartApp calls it "Rewarded Video")
    val rewardedEnabled: Boolean = false,

    // Splash Ad (StartApp specific)
    val splashEnabled: Boolean = false,
    val splashTheme: String = "DEFAULT", // DEFAULT, BLANK, TRANSPARENT
    val splashOrientation: String = "PORTRAIT", // PORTRAIT, LANDSCAPE

    // Auto-load
    val autoLoadInterstitial: Boolean = true,
    val autoLoadRewarded: Boolean = true,
    val preloadSplash: Boolean = true,

    // GDPR/Privacy
    val consentRequired: Boolean = false,
    val metadata: String = "" // JSON for additional config
)