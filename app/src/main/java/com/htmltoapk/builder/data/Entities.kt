package com.htmltoapk.builder.data

import androidx.room.*
import kotlinx.serialization.json.Json

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    var name: String = "",
    var packageName: String = "com.example.app",
    var versionCode: Int = 1,
    var versionName: String = "1.0",
    var appName: String = "",
    var contentSourceType: String = "local",
    var contentSourcePath: String = "",
    var contentSourceUrl: String = "",
    var sourcePath: String = "",
    var sourceUrl: String = "",
    var iconPath: String = "",
    var splashPath: String = "",
    var minSdk: Int = 24,
    var targetSdk: Int = 34,
    var maxSdk: Int? = null,
    var orientation: String = "portrait",
    var buildMode: String = "debug",
    var outputFormat: String = "apk",
    var primaryColor: String = "#6200EE",
    var showActionBar: Boolean = true,
    var showHeader: Boolean = true,
    var showWatermark: Boolean = false,
    var showCameraCutout: Boolean = false,
    var keepScreenOn: Boolean = false,
    var desktopMode: Boolean = false,
    var deepLinkScheme: String = "",
    var permissionsJson: String = "[]",
    var webViewSettingsJson: String = "{}",
    var admobJson: String = "{}",
    var billingJson: String = "{}",
    var umpJson: String = "{}",
    var unityAdsJson: String = "{}",
    var startAppJson: String = "{}",
    var keystoreId: Long? = null,
    var autoIncrementVersionCode: Boolean = false,
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
) {
    fun toProject(): Project {
        val json = Json { prettyPrint = true }
        return Project(
            id = id,
            name = name,
            packageName = packageName,
            versionCode = versionCode,
            versionName = versionName,
            appName = appName,
            contentSource = com.htmltoapk.builder.model.ContentSource.fromJson(json, contentSourceType, contentSourcePath, contentSourceUrl),
            sourcePath = sourcePath,
            sourceUrl = sourceUrl,
            iconPath = iconPath,
            splashPath = splashPath,
            minSdk = minSdk,
            targetSdk = targetSdk,
            maxSdk = maxSdk,
            orientation = orientation,
            buildMode = buildMode,
            outputFormat = outputFormat,
            primaryColor = primaryColor,
            showActionBar = showActionBar,
            showHeader = showHeader,
            showWatermark = showWatermark,
            showCameraCutout = showCameraCutout,
            keepScreenOn = keepScreenOn,
            desktopMode = desktopMode,
            deepLinkScheme = deepLinkScheme,
            permissions = json.decodeFromString(permissionsJson),
            webViewSettings = json.decodeFromString(webViewSettingsJson),
            admob = json.decodeFromString(admobJson),
            unityAds = json.decodeFromString(unityAdsJson),
            startApp = json.decodeFromString(startAppJson),
            billing = json.decodeFromString(billingJson),
            ump = json.decodeFromString(umpJson),
            keystoreId = keystoreId,
            autoIncrementVersionCode = autoIncrementVersionCode,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        private val json = Json { prettyPrint = true }

        fun fromProject(project: Project): ProjectEntity {
            return ProjectEntity(
                id = project.id,
                name = project.name,
                packageName = project.packageName,
                versionCode = project.versionCode,
                versionName = project.versionName,
                appName = project.appName,
                contentSourceType = project.contentSource.type.name,
                contentSourcePath = project.contentSource.path,
                contentSourceUrl = project.contentSource.url,
                sourcePath = project.sourcePath,
                sourceUrl = project.sourceUrl,
                iconPath = project.iconPath,
                splashPath = project.splashPath,
                minSdk = project.minSdk,
                targetSdk = project.targetSdk,
                maxSdk = project.maxSdk,
                orientation = project.orientation,
                buildMode = project.buildMode,
                outputFormat = project.outputFormat,
                primaryColor = project.primaryColor,
                showActionBar = project.showActionBar,
                showHeader = project.showHeader,
                showWatermark = project.showWatermark,
                showCameraCutout = project.showCameraCutout,
                keepScreenOn = project.keepScreenOn,
                desktopMode = project.desktopMode,
                deepLinkScheme = project.deepLinkScheme,
                permissionsJson = json.encodeToString(project.permissions),
                webViewSettingsJson = json.encodeToString(project.webViewSettings),
                admobJson = json.encodeToString(project.admob),
                unityAdsJson = json.encodeToString(project.unityAds),
                startAppJson = json.encodeToString(project.startApp),
                billingJson = json.encodeToString(project.billing),
                umpJson = json.encodeToString(project.ump),
                keystoreId = project.keystoreId,
                autoIncrementVersionCode = project.autoIncrementVersionCode,
                createdAt = project.createdAt,
                updatedAt = project.updatedAt
            )
        }
    }
}

@Entity(tableName = "keystores")
data class KeystoreEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    var name: String = "",
    var filePath: String = "",
    var alias: String = "",
    var password: String = "", // Encrypted
    var keyPassword: String = "", // Encrypted
    var isDefault: Boolean = false,
    var createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "build_history")
data class BuildHistoryEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    var projectId: Long = 0,
    var outputPath: String = "",
    var outputFormat: String = "apk",
    var success: Boolean = false,
    var errorMessage: String? = null,
    var buildDurationMs: Long = 0,
    var fileSizeBytes: Long = 0,
    var createdAt: Long = System.currentTimeMillis()
)