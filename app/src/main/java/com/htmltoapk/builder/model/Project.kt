package com.htmltoapk.builder.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Project(
    @Transient var id: Long = 0,
    var name: String = "",
    var packageName: String = "com.example.app",
    var versionCode: Int = 1,
    var versionName: String = "1.0",
    var appName: String = "",

    // Content source
    var contentSource: ContentSource = ContentSource.local(""),
    var sourcePath: String = "",
    var sourceUrl: String = "",

    // Assets
    var iconPath: String = "",
    var splashPath: String = "",

    // Build config
    var minSdk: Int = 24,
    var targetSdk: Int = 34,
    var maxSdk: Int? = null,
    var orientation: String = "portrait", // portrait, landscape, sensor
    var buildMode: String = "debug", // debug, release
    var outputFormat: String = "apk", // apk, aab

    // UI settings
    var primaryColor: String = "#6200EE",
    var showActionBar: Boolean = true,
    var showHeader: Boolean = true,
    var showWatermark: Boolean = false,
    var showCameraCutout: Boolean = false,
    var keepScreenOn: Boolean = false,
    var desktopMode: Boolean = false,
    var deepLinkScheme: String = "",

    // Permissions
    var permissions: List<String> = emptyList(),

    // WebView settings
    var webViewSettings: WebViewSettings = WebViewSettings.DEFAULT,

    // AdMob
    var admob: AdMobSettings = AdMobSettings(),

    // Unity Ads
    var unityAds: UnityAdsSettings = UnityAdsSettings(),

    // StartApp (Start.io)
    var startApp: StartAppSettings = StartAppSettings(),

    // Billing
    var billing: BillingSettings = BillingSettings(),

    // UMP
    var ump: UMPSettings = UMPSettings(),

    // Keystore
    var keystoreId: Long? = null,

    // Version management
    var autoIncrementVersionCode: Boolean = false,

    // Timestamps
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
) {

    fun copyWith(
        name: String? = null,
        packageName: String? = null,
        versionCode: Int? = null,
        versionName: String? = null,
        appName: String? = null,
        contentSource: ContentSource? = null,
        sourcePath: String? = null,
        sourceUrl: String? = null,
        iconPath: String? = null,
        splashPath: String? = null,
        minSdk: Int? = null,
        targetSdk: Int? = null,
        maxSdk: Int? = null,
        orientation: String? = null,
        buildMode: String? = null,
        outputFormat: String? = null,
        primaryColor: String? = null,
        showActionBar: Boolean? = null,
        showHeader: Boolean? = null,
        showWatermark: Boolean? = null,
        showCameraCutout: Boolean? = null,
        keepScreenOn: Boolean? = null,
        desktopMode: Boolean? = null,
        deepLinkScheme: String? = null,
        permissions: List<String>? = null,
        webViewSettings: WebViewSettings? = null,
        admob: AdMobSettings? = null,
        unityAds: UnityAdsSettings? = null,
        startApp: StartAppSettings? = null,
        billing: BillingSettings? = null,
        ump: UMPSettings? = null,
        keystoreId: Long? = null,
        autoIncrementVersionCode: Boolean? = null
    ): Project {
        return copy(
            name = name ?: this.name,
            packageName = packageName ?: this.packageName,
            versionCode = versionCode ?: this.versionCode,
            versionName = versionName ?: this.versionName,
            appName = appName ?: this.appName,
            contentSource = contentSource ?: this.contentSource,
            sourcePath = sourcePath ?: this.sourcePath,
            sourceUrl = sourceUrl ?: this.sourceUrl,
            iconPath = iconPath ?: this.iconPath,
            splashPath = splashPath ?: this.splashPath,
            minSdk = minSdk ?: this.minSdk,
            targetSdk = targetSdk ?: this.targetSdk,
            maxSdk = maxSdk ?: this.maxSdk,
            orientation = orientation ?: this.orientation,
            buildMode = buildMode ?: this.buildMode,
            outputFormat = outputFormat ?: this.outputFormat,
            primaryColor = primaryColor ?: this.primaryColor,
            showActionBar = showActionBar ?: this.showActionBar,
            showHeader = showHeader ?: this.showHeader,
            showWatermark = showWatermark ?: this.showWatermark,
            showCameraCutout = showCameraCutout ?: this.showCameraCutout,
            keepScreenOn = keepScreenOn ?: this.keepScreenOn,
            desktopMode = desktopMode ?: this.desktopMode,
            deepLinkScheme = deepLinkScheme ?: this.deepLinkScheme,
            permissions = permissions ?: this.permissions,
            webViewSettings = webViewSettings ?: this.webViewSettings,
            admob = admob ?: this.admob,
            unityAds = unityAds ?: this.unityAds,
            startApp = startApp ?: this.startApp,
            billing = billing ?: this.billing,
            ump = ump ?: this.ump,
            keystoreId = keystoreId ?: this.keystoreId,
            autoIncrementVersionCode = autoIncrementVersionCode ?: this.autoIncrementVersionCode,
            updatedAt = System.currentTimeMillis()
        )
    }

    companion object {
        val json = Json { prettyPrint = true }

        fun fromJson(jsonString: String): Project = json.decodeFromString(jsonString)

        fun toJson(project: Project): String = json.encodeToString(project)

        fun createDefault(name: String = "My App"): Project {
            return Project(
                name = name,
                appName = name,
                packageName = "com.example.${name.lowercase().replace(" ", "")}",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }
    }
}

@Serializable
data class BuildConfiguration(
    val project: Project,
    val keystorePath: String = "",
    val keystorePassword: String = "",
    val keyAlias: String = "",
    val keyPassword: String = "",
    val outputFormat: String = "apk", // apk, aab
    val useCustomKeystore: Boolean = false
    // NO isPremium field - all features free
)