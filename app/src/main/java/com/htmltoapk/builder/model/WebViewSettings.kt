package com.htmltoapk.builder.model

import kotlinx.serialization.Serializable

@Serializable
data class WebViewSettings(
    val javascriptEnabled: Boolean = true,
    val domStorageEnabled: Boolean = true,
    val databaseEnabled: Boolean = true,
    val mixedContentMode: Int = 0, // 0 = never, 1 = always, 2 = compatible
    val userAgent: String = "",
    val allowFileAccess: Boolean = true,
    val allowContentAccess: Boolean = true,
    val allowUniversalAccess: Boolean = false,
    val cacheMode: Int = -1, // -1 = default
    val thirdPartyCookiesEnabled: Boolean = true,
    val safeBrowsingEnabled: Boolean = true
) {
    companion object {
        val DEFAULT = WebViewSettings()
    }
}