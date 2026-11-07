package com.htmltoapk.builder.data

import androidx.room.*
import com.htmltoapk.builder.model.Project
import kotlinx.coroutines.flow.Flow
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

@Dao
interface ProjectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: ProjectEntity): Long

    @Update
    suspend fun update(project: ProjectEntity)

    @Delete
    suspend fun delete(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun getProjectById(id: Long): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE packageName = :packageName")
    suspend fun getProjectByPackageName(packageName: String): ProjectEntity?
}

@Dao
interface KeystoreDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(keystore: KeystoreEntity): Long

    @Update
    suspend fun update(keystore: KeystoreEntity)

    @Delete
    suspend fun delete(keystore: KeystoreEntity)

    @Query("SELECT * FROM keystores ORDER BY createdAt DESC")
    fun getAllKeystores(): Flow<List<KeystoreEntity>>

    @Query("SELECT * FROM keystores WHERE isDefault = 1")
    suspend fun getDefaultKeystore(): KeystoreEntity?

    @Query("SELECT * FROM keystores WHERE id = :id")
    suspend fun getKeystoreById(id: Long): KeystoreEntity?
}

@Dao
interface BuildHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: BuildHistoryEntity): Long

    @Query("SELECT * FROM build_history WHERE projectId = :projectId ORDER BY createdAt DESC LIMIT 50")
    fun getHistoryForProject(projectId: Long): Flow<List<BuildHistoryEntity>>

    @Query("SELECT * FROM build_history ORDER BY createdAt DESC LIMIT 100")
    fun getRecentBuilds(): Flow<List<BuildHistoryEntity>>
}

@Database(
    entities = [ProjectEntity::class, KeystoreEntity::class, BuildHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun keystoreDao(): KeystoreDao
    abstract fun buildHistoryDao(): BuildHistoryDao
}

import kotlinx.serialization.json.Json

fun ProjectEntity.toProject(): Project {
    val json = Json { prettyPrint = true }
    return Project(
        id = id,
        name = name,
        packageName = packageName,
        versionCode = versionCode,
        versionName = versionName,
        appName = appName,
        contentSource = if (contentSourceType == "remote") {
            Project.ContentSource.Remote(contentSourceUrl)
        } else {
            Project.ContentSource.Local(contentSourcePath.ifBlank { sourcePath })
        },
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
        billing = json.decodeFromString(billingJson),
        ump = json.decodeFromString(umpJson),
        unityAds = json.decodeFromString(unityAdsJson),
        startApp = json.decodeFromString(startAppJson),
        keystoreId = keystoreId,
        autoIncrementVersionCode = autoIncrementVersionCode,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}