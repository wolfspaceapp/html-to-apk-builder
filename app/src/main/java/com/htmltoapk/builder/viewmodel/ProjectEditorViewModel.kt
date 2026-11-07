package com.htmltoapk.builder.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.htmltoapk.builder.HtmlToApkApplication
import com.htmltoapk.builder.data.AppDatabase
import com.htmltoapk.builder.data.KeystoreEntity
import kotlinx.coroutines.flow.collect

class ProjectEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase = HtmlToApkApplication.getInstance().getDatabase()

    val keystores: LiveData<List<KeystoreEntity>> = database.keystoreDao().getAllKeystores().asLiveData()
}