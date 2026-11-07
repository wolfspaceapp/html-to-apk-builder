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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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
        // Build JsonObject manually to avoid Map<String, Any> serialization issues
        val config = buildJsonObject {
            put("packageName", JsonPrimitive(project.packageName))
            put("appName", JsonPrimitive(project.appName.ifBlank { project.name }))
            put("versionCode", JsonPrimitive(project.versionCode))
            put("versionName", JsonPrimitive(project.versionName))
            put("orientation", JsonPrimitive(project.orientation))
            put("themeColor", JsonPrimitive(project.primaryColor))
            put("showActionBar", JsonPrimitive(project.showActionBar))
            put("showHeader", JsonPrimitive(project.showHeader))
            put("showWatermark", JsonPrimitive(project.showWatermark))
            put("keepScreenOn", JsonPrimitive(project.keepScreenOn))
            put("desktopMode", JsonPrimitive(project.desktopMode))
            put("deepLinkScheme", JsonPrimitive(project.deepLinkScheme))
            put("permissions", JsonPrimitive(project.permissions))
            put("webViewSettings", json.parseToJsonElement(project.webViewSettings))
            put("admob", buildJsonObject {
                put("enabled", JsonPrimitive(project.admob.enabled))
                put("appId", JsonPrimitive(project.admob.appId))
                put("bannerEnabled", JsonPrimitive(project.admob.bannerEnabled))
                put("bannerUnitId", JsonPrimitive(project.admob.bannerUnitId))
                put("bannerPosition", JsonPrimitive(project.admob.bannerPosition))
                put("showBannerOnStart", JsonPrimitive(project.admob.showBannerOnStart))
                put("interstitialEnabled", JsonPrimitive(project.admob.interstitialEnabled))
                put("interstitialUnitId", JsonPrimitive(project.admob.interstitialUnitId))
                put("interstitialIntervalSeconds", JsonPrimitive(project.admob.interstitialIntervalSeconds))
                put("interstitialPageCount", JsonPrimitive(project.admob.interstitialPageCount))
                put("rewardedEnabled", JsonPrimitive(project.admob.rewardedEnabled))
                put("rewardedUnitId", JsonPrimitive(project.admob.rewardedUnitId))
                put("autoLoadInterstitial", JsonPrimitive(project.admob.autoLoadInterstitial))
                put("autoLoadRewarded", JsonPrimitive(project.admob.autoLoadRewarded))
                put("useTestAds", JsonPrimitive(project.admob.useTestAds))
                put("testDeviceIds", JsonPrimitive(project.admob.testDeviceIds))
                put("maxAdContentRating", JsonPrimitive(project.admob.maxAdContentRating))
                put("childDirectedTreatment", JsonPrimitive(project.admob.childDirectedTreatment))
                put("underAgeOfConsent", JsonPrimitive(project.admob.underAgeOfConsent))
            })
            put("unityAds", buildJsonObject {
                put("enabled", JsonPrimitive(project.unityAds.enabled))
                put("gameId", JsonPrimitive(project.unityAds.gameId))
                put("testMode", JsonPrimitive(project.unityAds.testMode))
                put("bannerEnabled", JsonPrimitive(project.unityAds.bannerEnabled))
                put("bannerPlacementId", JsonPrimitive(project.unityAds.bannerPlacementId))
                put("bannerPosition", JsonPrimitive(project.unityAds.bannerPosition))
                put("interstitialEnabled", JsonPrimitive(project.unityAds.interstitialEnabled))
                put("interstitialPlacementId", JsonPrimitive(project.unityAds.interstitialPlacementId))
                put("interstitialIntervalSeconds", JsonPrimitive(project.unityAds.interstitialIntervalSeconds))
                put("interstitialPageCount", JsonPrimitive(project.unityAds.interstitialPageCount))
                put("rewardedEnabled", JsonPrimitive(project.unityAds.rewardedEnabled))
                put("rewardedPlacementId", JsonPrimitive(project.unityAds.rewardedPlacementId))
                put("autoLoadInterstitial", JsonPrimitive(project.unityAds.autoLoadInterstitial))
                put("autoLoadRewarded", JsonPrimitive(project.unityAds.autoLoadRewarded))
                put("consentRequired", JsonPrimitive(project.unityAds.consentRequired))
                put("metadata", JsonPrimitive(project.unityAds.metadata))
            })
            put("startApp", buildJsonObject {
                put("enabled", JsonPrimitive(project.startApp.enabled))
                put("appId", JsonPrimitive(project.startApp.appId))
                put("devId", JsonPrimitive(project.startApp.devId))
                put("testMode", JsonPrimitive(project.startApp.testMode))
                put("bannerEnabled", JsonPrimitive(project.startApp.bannerEnabled))
                put("bannerType", JsonPrimitive(project.startApp.bannerType))
                put("bannerPosition", JsonPrimitive(project.startApp.bannerPosition))
                put("interstitialEnabled", JsonPrimitive(project.startApp.interstitialEnabled))
                put("interstitialIntervalSeconds", JsonPrimitive(project.startApp.interstitialIntervalSeconds))
                put("interstitialPageCount", JsonPrimitive(project.startApp.interstitialPageCount))
                put("rewardedEnabled", JsonPrimitive(project.startApp.rewardedEnabled))
                put("splashEnabled", JsonPrimitive(project.startApp.splashEnabled))
                put("splashTheme", JsonPrimitive(project.startApp.splashTheme))
                put("splashOrientation", JsonPrimitive(project.startApp.splashOrientation))
                put("autoLoadInterstitial", JsonPrimitive(project.startApp.autoLoadInterstitial))
                put("autoLoadRewarded", JsonPrimitive(project.startApp.autoLoadRewarded))
                put("preloadSplash", JsonPrimitive(project.startApp.preloadSplash))
                put("consentRequired", JsonPrimitive(project.startApp.consentRequired))
                put("metadata", JsonPrimitive(project.startApp.metadata))
            })
            put("billing", buildJsonObject {
                put("enabled", JsonPrimitive(project.billing.enabled))
                put("type", JsonPrimitive(project.billing.type))
                put("productId", JsonPrimitive(project.billing.productId))
                put("productName", JsonPrimitive(project.billing.productName))
                put("productDescription", JsonPrimitive(project.billing.productDescription))
                put("subscriptionId", JsonPrimitive(project.billing.subscriptionId))
                put("subscriptionPeriod", JsonPrimitive(project.billing.subscriptionPeriod))
                put("freeTrialDays", JsonPrimitive(project.billing.freeTrialDays))
                put("introductoryPrice", JsonPrimitive(project.billing.introductoryPrice))
                put("acknowledge", JsonPrimitive(project.billing.acknowledge))
                put("autoConsume", JsonPrimitive(project.billing.autoConsume))
                put("enablePendingPurchases", JsonPrimitive(project.billing.enablePendingPurchases))
                put("testMode", JsonPrimitive(project.billing.testMode))
            })
            put("ump", buildJsonObject {
                put("debugEnabled", JsonPrimitive(project.ump.debugEnabled))
                put("debugGeography", JsonPrimitive(project.ump.debugGeography))
                put("testDeviceIds", JsonPrimitive(project.ump.testDeviceIds))
            })
        }

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