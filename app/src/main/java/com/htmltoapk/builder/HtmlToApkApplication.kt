package com.htmltoapk.builder

import android.app.Application
import androidx.room.Room
import com.htmltoapk.builder.data.AppDatabase
import com.jakewharton.timber.Timber
import timber.log.Timber

class HtmlToApkApplication : Application() {

    companion object {
        @Volatile
        private var INSTANCE: HtmlToApkApplication? = null

        fun getInstance(): HtmlToApkApplication = INSTANCE!!
    }

    private var appDatabase: AppDatabase? = null

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this

        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    fun getDatabase(): AppDatabase {
        return appDatabase ?: synchronized(this) {
            appDatabase ?: Room.databaseBuilder(
                this,
                AppDatabase::class.java,
                "html_to_apk_builder.db"
            ).build().also { appDatabase = it }
        }
    }
}