package com.htmltoapk.builder.data

import com.htmltoapk.builder.model.Project
import com.htmltoapk.builder.model.ContentSource
import kotlinx.serialization.json.Json

object EntityMapper {
    private val json = Json { prettyPrint = true }

    fun ProjectEntity.toProject(): Project {
        return Project(
            id = id,
            name = name,
            packageName = packageName,
            versionCode = versionCode,
            versionName = versionName,
            appName = appName,
            contentSource = ContentSource.fromJson(json, contentSourceType, contentSourcePath, contentSourceUrl),
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

    fun Project.toEntity(): ProjectEntity {
        return ProjectEntity(
            id = id,
            name = name,
            packageName = packageName,
            versionCode = versionCode,
            versionName = versionName,
            appName = appName,
            contentSourceType = contentSource.type.name,
            contentSourcePath = contentSource.path,
            contentSourceUrl = contentSource.url,
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
            permissionsJson = json.encodeToString(permissions),
            webViewSettingsJson = json.encodeToString(webViewSettings),
            admobJson = json.encodeToString(admob),
            unityAdsJson = json.encodeToString(unityAds),
            startAppJson = json.encodeToString(startApp),
            billingJson = json.encodeToString(billing),
            umpJson = json.encodeToString(ump),
            keystoreId = keystoreId,
            autoIncrementVersionCode = autoIncrementVersionCode,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}