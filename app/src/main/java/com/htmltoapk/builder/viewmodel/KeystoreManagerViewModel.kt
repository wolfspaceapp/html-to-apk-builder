package com.htmltoapk.builder.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.htmltoapk.builder.HtmlToApkApplication
import com.htmltoapk.builder.data.AppDatabase
import com.htmltoapk.builder.data.KeystoreEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class KeystoreManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase = HtmlToApkApplication.getInstance().getDatabase()

    val keystores: LiveData<List<KeystoreEntity>> = database.keystoreDao().getAllKeystores().asLiveData()

    fun insertKeystore(keystore: KeystoreEntity) {
        viewModelScope.launch {
            // If this is the first keystore or marked as default, unset others
            if (keystore.isDefault) {
                unsetAllDefaults()
            }
            database.keystoreDao().insert(keystore)
        }
    }

    fun updateKeystore(keystore: KeystoreEntity) {
        viewModelScope.launch {
            if (keystore.isDefault) {
                unsetAllDefaults()
            }
            database.keystoreDao().update(keystore)
        }
    }

    fun deleteKeystore(keystoreId: Long) {
        viewModelScope.launch {
            database.keystoreDao().delete(KeystoreEntity(id = keystoreId))
        }
    }

    fun setDefaultKeystore(keystoreId: Long) {
        viewModelScope.launch {
            unsetAllDefaults()
            val keystore = database.keystoreDao().getKeystoreById(keystoreId)
            keystore?.let {
                database.keystoreDao().update(it.copy(isDefault = true))
            }
        }
    }

    private suspend fun unsetAllDefaults() {
        // This would need a DAO query to update all to false
        // For simplicity, we'll do it in a transaction
        database.runInTransaction {
            val allKeystores = database.keystoreDao().getAllKeystores().first() ?: return@runInTransaction
            allKeystores.forEach { ks ->
                if (ks.isDefault) {
                    database.keystoreDao().update(ks.copy(isDefault = false))
                }
            }
        }
    }
}