package com.htmltoapk.builder.data

import androidx.room.*

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
)

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