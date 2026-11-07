package com.htmltoapk.builder.template

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.htmltoapk.builder.model.Project
import com.htmltoapk.builder.model.WebViewSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.InputStream

object ProjectAssetWriter {
    private const val TAG = "ProjectAssetWriter"
    private val json = Json { prettyPrint = true }

    suspend fun writeProjectAssets(
        context: Context,
        project: Project,
        templateDir: File,
        includeSource: Boolean = true
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val assetsDir = File(templateDir, "assets")
                assetsDir.mkdirs()

                // Write config.json
                writeConfig(assetsDir, project)

                // Write web view settings
                writeWebViewSettings(assetsDir, project)

                // Write source files
                if (includeSource) {
                    writeSourceFiles(context, project, assetsDir)
                }

                // Copy icons
                copyIcons(context, project, templateDir)

                // Copy splash screens
                copySplash(context, project, templateDir)

                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write project assets", e)
                false
            }
        }
    }

    private fun writeConfig(assetsDir: File, project: Project) {
        val config = mapOf(
            "packageName" to project.packageName,
            "appName" to project.appName.ifBlank { project.name },
            "versionCode" to project.versionCode,
            "versionName" to project.versionName,
            "orientation" to project.orientation,
            "themeColor" to project.primaryColor,
            "showActionBar" to project.showActionBar,
            "showHeader" to project.showHeader,
            "showWatermark" to project.showWatermark,
            "keepScreenOn" to project.keepScreenOn,
            "desktopMode" to project.desktopMode,
            "deepLinkScheme" to project.deepLinkScheme,
            "permissions" to project.permissions,
            "webViewSettings" to json.decodeFromString<Map<String, Any>>(json.encodeToString(project.webViewSettings)),
            "admob" to mapOf(
                "enabled" to project.admob.enabled,
                "appId" to project.admob.appId,
                "bannerEnabled" to project.admob.bannerEnabled,
                "bannerUnitId" to project.admob.bannerUnitId,
                "bannerPosition" to project.admob.bannerPosition,
                "showBannerOnStart" to project.admob.showBannerOnStart,
                "interstitialEnabled" to project.admob.interstitialEnabled,
                "interstitialUnitId" to project.admob.interstitialUnitId,
                "interstitialIntervalSeconds" to project.admob.interstitialIntervalSeconds,
                "interstitialPageCount" to project.admob.interstitialPageCount,
                "rewardedEnabled" to project.admob.rewardedEnabled,
                "rewardedUnitId" to project.admob.rewardedUnitId,
                "autoLoadInterstitial" to project.admob.autoLoadInterstitial,
                "autoLoadRewarded" to project.admob.autoLoadRewarded,
                "useTestAds" to project.admob.useTestAds,
                "testDeviceIds" to project.admob.testDeviceIds,
                "maxAdContentRating" to project.admob.maxAdContentRating,
                "childDirectedTreatment" to project.admob.childDirectedTreatment,
                "underAgeOfConsent" to project.admob.underAgeOfConsent
            ),
            "unityAds" to mapOf(
                "enabled" to project.unityAds.enabled,
                "gameId" to project.unityAds.gameId,
                "testMode" to project.unityAds.testMode,
                "bannerEnabled" to project.unityAds.bannerEnabled,
                "bannerPlacementId" to project.unityAds.bannerPlacementId,
                "bannerPosition" to project.unityAds.bannerPosition,
                "interstitialEnabled" to project.unityAds.interstitialEnabled,
                "interstitialPlacementId" to project.unityAds.interstitialPlacementId,
                "interstitialIntervalSeconds" to project.unityAds.interstitialIntervalSeconds,
                "interstitialPageCount" to project.unityAds.interstitialPageCount,
                "rewardedEnabled" to project.unityAds.rewardedEnabled,
                "rewardedPlacementId" to project.unityAds.rewardedPlacementId,
                "autoLoadInterstitial" to project.unityAds.autoLoadInterstitial,
                "autoLoadRewarded" to project.unityAds.autoLoadRewarded,
                "consentRequired" to project.unityAds.consentRequired,
                "metadata" to project.unityAds.metadata
            ),
            "startApp" to mapOf(
                "enabled" to project.startApp.enabled,
                "appId" to project.startApp.appId,
                "devId" to project.startApp.devId,
                "testMode" to project.startApp.testMode,
                "bannerEnabled" to project.startApp.bannerEnabled,
                "bannerType" to project.startApp.bannerType,
                "bannerPosition" to project.startApp.bannerPosition,
                "interstitialEnabled" to project.startApp.interstitialEnabled,
                "interstitialIntervalSeconds" to project.startApp.interstitialIntervalSeconds,
                "interstitialPageCount" to project.startApp.interstitialPageCount,
                "rewardedEnabled" to project.startApp.rewardedEnabled,
                "splashEnabled" to project.startApp.splashEnabled,
                "splashTheme" to project.startApp.splashTheme,
                "splashOrientation" to project.startApp.splashOrientation,
                "autoLoadInterstitial" to project.startApp.autoLoadInterstitial,
                "autoLoadRewarded" to project.startApp.autoLoadRewarded,
                "preloadSplash" to project.startApp.preloadSplash,
                "consentRequired" to project.startApp.consentRequired,
                "metadata" to project.startApp.metadata
            ),
            "billing" to mapOf(
                "enabled" to project.billing.enabled,
                "type" to project.billing.type,
                "productId" to project.billing.productId,
                "productName" to project.billing.productName,
                "productDescription" to project.billing.productDescription,
                "subscriptionId" to project.billing.subscriptionId,
                "subscriptionPeriod" to project.billing.subscriptionPeriod,
                "freeTrialDays" to project.billing.freeTrialDays,
                "introductoryPrice" to project.billing.introductoryPrice,
                "acknowledge" to project.billing.acknowledge,
                "autoConsume" to project.billing.autoConsume,
                "enablePendingPurchases" to project.billing.enablePendingPurchases,
                "testMode" to project.billing.testMode
            ),
            "ump" to mapOf(
                "debugEnabled" to project.ump.debugEnabled,
                "debugGeography" to project.ump.debugGeography,
                "testDeviceIds" to project.ump.testDeviceIds
            )
        )

        val configFile = File(assetsDir, "config.json")
        FileWriter(configFile).use { it.write(json.encodeToString(config)) }
    }

    private fun writeWebViewSettings(assetsDir: File, project: Project) {
        val settingsFile = File(assetsDir, "webview_settings.json")
        FileWriter(settingsFile).use { it.write(json.encodeToString(project.webViewSettings)) }
    }

    private fun writeSourceFiles(context: Context, project: Project, assetsDir: File) {
        val wwwDir = File(assetsDir, "www")
        wwwDir.mkdirs()

        when (project.contentSource) {
            is Project.ContentSource.Local -> {
                val sourcePath = project.sourcePath.ifBlank { project.contentSource.path }
                if (sourcePath.isNotBlank()) {
                    copySourceFiles(context, sourcePath, wwwDir)
                } else {
                    createDefaultIndex(wwwDir, project)
                }
            }
            is Project.ContentSource.Remote -> {
                // For remote, create a redirect page
                createRemoteIndex(wwwDir, project)
            }
        }
    }

    private fun copySourceFiles(context: Context, sourcePath: String, destDir: File) {
        val sourceFile = File(sourcePath)
        if (!sourceFile.exists()) {
            // Try to resolve as content URI
            try {
                val uri = Uri.parse(sourcePath)
                if (uri.scheme == "content") {
                    copyFromContentUri(context, uri, destDir)
                    return
                }
            } catch (e: Exception) {
                // Ignore
            }
            createDefaultIndex(destDir, Project.createDefault())
            return
        }

        if (sourceFile.isFile) {
            sourceFile.copyTo(File(destDir, sourceFile.name), overwrite = true)
        } else {
            copyDirectory(sourceFile, destDir)
        }
    }

    private fun copyFromContentUri(context: Context, uri: Uri, destDir: File) {
        context.contentResolver.openInputStream(uri).use { input ->
            if (input != null) {
                val destFile = File(destDir, "index.html")
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    private fun copyDirectory(srcDir: File, destDir: File) {
        srcDir.walkTopDown().forEach { srcFile ->
            val relativePath = srcFile.path.substring(srcDir.path.length + 1)
            val destFile = File(destDir, relativePath)
            if (srcFile.isDirectory) {
                destFile.mkdirs()
            } else {
                destFile.parentFile?.mkdirs()
                srcFile.copyTo(destFile, overwrite = true)
            }
        }
    }

    private fun createDefaultIndex(wwwDir: File, project: Project) {
        val html = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${project.appName.ifBlank { project.name }}</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            margin: 0;
            padding: 20px;
            background: linear-gradient(135deg, ${project.primaryColor}22, ${project.primaryColor}44);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        .container {
            text-align: center;
            background: white;
            padding: 40px;
            border-radius: 16px;
            box-shadow: 0 4px 20px rgba(0,0,0,0.1);
            max-width: 400px;
        }
        h1 { color: ${project.primaryColor}; margin-bottom: 16px; }
        p { color: #666; line-height: 1.6; }
        .badge {
            display: inline-block;
            background: ${project.primaryColor};
            color: white;
            padding: 8px 16px;
            border-radius: 20px;
            font-size: 14px;
            margin-top: 20px;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>${project.appName.ifBlank { project.name }}</h1>
        <p>Your HTML-to-APK app is ready! Replace this content with your own HTML, CSS, and JavaScript files.</p>
        <div class="badge">Package: ${project.packageName}</div>
        <div class="badge">Version: ${project.versionName} (${project.versionCode})</div>
    </div>
</body>
</html>
        """.trimIndent()

        File(wwwDir, "index.html").writeText(html)
    }

    private fun createRemoteIndex(wwwDir: File, project: Project) {
        val html = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${project.appName.ifBlank { project.name }}</title>
    <script>
        // Redirect to remote URL
        window.location.href = "${project.contentSource.url}";
    </script>
</head>
<body>
    <p>Loading ${project.appName.ifBlank { project.name }}...</p>
    <p><a href="${project.contentSource.url}">Click here if not redirected</a></p>
</body>
</html>
        """.trimIndent()

        File(wwwDir, "index.html").writeText(html)
    }

    private fun copyIcons(context: Context, project: Project, templateDir: File) {
        val iconPath = project.iconPath.ifBlank { return }
        val iconFile = resolveIconFile(context, iconPath)
        if (iconFile == null || !iconFile.exists()) return

        val densities = mapOf(
            "mipmap-mdpi" to 48,
            "mipmap-hdpi" to 72,
            "mipmap-xhdpi" to 96,
            "mipmap-xxhdpi" to 144,
            "mipmap-xxxhdpi" to 192
        )

        densities.forEach { (dirName, size) ->
            val destDir = File(templateDir, "res/$dirName")
            destDir.mkdirs()
            val destFile = File(destDir, "ic_launcher.png")
            resizeAndSaveIcon(iconFile, destFile, size)

            // Round icon
            val roundFile = File(destDir, "ic_launcher_round.png")
            resizeAndSaveIcon(iconFile, roundFile, size)
        }
    }

    private fun resolveIconFile(context: Context, path: String): File? {
        return try {
            val file = File(path)
            if (file.exists()) file else {
                val uri = Uri.parse(path)
                if (uri.scheme == "content") {
                    val tempFile = File(context.cacheDir, "temp_icon.png")
                    context.contentResolver.openInputStream(uri).use { input ->
                        input?.copyTo(FileOutputStream(tempFile))
                    }
                    tempFile
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun resizeAndSaveIcon(srcFile: File, destFile: File, size: Int) {
        try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(srcFile.absolutePath, options)

            options.inJustDecodeBounds = false
            options.inSampleSize = calculateInSampleSize(options, size, size)

            val bitmap = BitmapFactory.decodeFile(srcFile.absolutePath, options)
            val resized = Bitmap.createScaledBitmap(bitmap, size, size, true)

            FileOutputStream(destFile).use { output ->
                resized.compress(Bitmap.CompressFormat.PNG, 100, output)
            }

            bitmap.recycle()
            resized.recycle()
        } catch (e: Exception) {
            // Fallback: copy original
            srcFile.copyTo(destFile, overwrite = true)
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
            val halfHeight = options.outHeight / 2
            val halfWidth = options.outWidth / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun copySplash(context: Context, project: Project, templateDir: File) {
        val splashPath = project.splashPath.ifBlank { return }
        val splashFile = resolveIconFile(context, splashPath)
        if (splashFile == null || !splashFile.exists()) return

        val densities = listOf("drawable-mdpi", "drawable-hdpi", "drawable-xhdpi", "drawable-xxhdpi", "drawable-xxxhdpi")
        densities.forEach { dirName ->
            val destDir = File(templateDir, "res/$dirName")
            destDir.mkdirs()
            val destFile = File(destDir, "splash.png")
            splashFile.copyTo(destFile, overwrite = true)
        }
    }
}