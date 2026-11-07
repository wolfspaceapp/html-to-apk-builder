package com.htmltoapk.builder.data

import androidx.room.*
import com.htmltoapk.builder.model.Project
import kotlinx.serialization.json.Json

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
    suspend fun getProjectById(id: Long): ProjectEntity?

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