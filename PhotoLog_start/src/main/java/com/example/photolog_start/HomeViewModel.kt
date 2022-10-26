/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.photolog_start

import android.app.Application
import android.content.Context
import android.text.format.DateUtils
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.room.Room
import com.example.photolog_start.AppDatabase.Companion.DB_NAME
import kotlinx.coroutines.launch

class HomeViewModel(
    application: Application,
    private val photoSaver: PhotoSaverRepository
) : AndroidViewModel(application) {

    private val context: Context
        get() = getApplication()

    private val db = Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME).build()

    data class UiState(val loading: Boolean = true, val logs: List<Log> = emptyList())

    var uiState by mutableStateOf(UiState())
        private set

    fun formatDateTime(timeInMillis: Long): String {
        return DateUtils.formatDateTime(context, timeInMillis, DateUtils.FORMAT_ABBREV_ALL)
    }

    fun loadLogs() {
        viewModelScope.launch {
            uiState = uiState.copy(
                loading = false,
                logs = db.logDao().getAllWithFiles(photoSaver.photoFolder)
            )
        }
    }

    fun delete(log: Log) {
        viewModelScope.launch {
            db.logDao().delete(log.toLogEntry())
            loadLogs()
        }
    }
}

class HomeViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val app =
            extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as PhotoLogApplication
        return HomeViewModel(app, app.photoSaver) as T
    }
}