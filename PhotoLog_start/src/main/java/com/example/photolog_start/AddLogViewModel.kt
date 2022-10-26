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

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.room.Room
import com.example.photolog_start.AppDatabase.Companion.DB_NAME
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddLogViewModel(
    application: Application,
    private val photoSaver: PhotoSaverRepository
) : AndroidViewModel(application) {
    // region ViewModel setup
    private val context: Context
        get() = getApplication()

    private val mediaRepository = MediaRepository(context)
    private val db = Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME).build()
    // endregion

    // region UI state
    data class UiState(
        val hasLocationAccess: Boolean,
        val hasCameraAccess: Boolean,
        val isSaving: Boolean = false,
        val isSaved: Boolean = false,
        val date: Long,
        val place: String? = null,
        val savedPhotos: List<File> = emptyList(),
        val localPickerPhotos: List<Uri> = emptyList()
    )

    var uiState by mutableStateOf(
        UiState(
            hasLocationAccess = hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION),
            hasCameraAccess = hasPermission(Manifest.permission.CAMERA),
            date = getTodayDateInMillis(),
            savedPhotos = photoSaver.getPhotos()
        )
    )
        private set

    fun isValid(): Boolean {
        return uiState.place != null && !photoSaver.isEmpty() && !uiState.isSaving
    }

    private fun getTodayDateInMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        return calendar.timeInMillis
    }

    private fun getIsoDate(timeInMillis: Long): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(timeInMillis)
    }

    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun onPermissionChange(permission: String, isGranted: Boolean) {
        when (permission) {
            Manifest.permission.ACCESS_COARSE_LOCATION -> {
                uiState = uiState.copy(hasLocationAccess = isGranted)
            }
            Manifest.permission.ACCESS_FINE_LOCATION -> {
                uiState = uiState.copy(hasLocationAccess = isGranted)
            }
            Manifest.permission.CAMERA -> {
                uiState = uiState.copy(hasCameraAccess = isGranted)
            }
            else -> {
                Log.e("Permission change", "Unexpected permission: $permission")
            }
        }
    }

    fun onDateChange(dateInMillis: Long) {
        uiState = uiState.copy(date = dateInMillis)
    }

    fun createSettingsIntent(): Intent {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            data = Uri.fromParts("package", context.packageName, null)
        }

        return intent
    }
    // endregion

    // region Location management
    @SuppressLint("MissingPermission")
    fun fetchLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location ?: return@addOnSuccessListener

            val geocoder = Geocoder(context, Locale.getDefault())
            val address =
                geocoder.getFromLocation(location.latitude, location.longitude, 1).firstOrNull()
                    ?: return@addOnSuccessListener
            val place =
                address.locality ?: address.subAdminArea ?: address.adminArea ?: address.countryName
                ?: return@addOnSuccessListener

            uiState = uiState.copy(place = place)
        }
    }
    // endregion

    fun loadLocalPickerPictures() {
        viewModelScope.launch {
            val localPickerPhotos = mediaRepository.fetchImages().map { it.uri }.toList()
            uiState = uiState.copy(localPickerPhotos = localPickerPhotos)
        }
    }

    fun onLocalPhotoPickerSelect(photo: Uri) {
        viewModelScope.launch {
            photoSaver.cacheFromUri(photo)
            refreshSavedPhotos()
        }
    }

    fun onPhotoPickerSelect(photos: List<Uri>) {
        viewModelScope.launch {
            photoSaver.cacheFromUris(photos)
            refreshSavedPhotos()
        }
    }

    // region Photo management

    fun canAddPhoto() = photoSaver.canAddPhoto()

    fun refreshSavedPhotos() {
        uiState = uiState.copy(savedPhotos = photoSaver.getPhotos())
    }

    fun onPhotoRemoved(photo: File) {
        viewModelScope.launch {
            photoSaver.removeFile(photo)
            refreshSavedPhotos()
        }
    }

    fun createLog() {
        if (!isValid()) {
            return
        }

        uiState = uiState.copy(isSaving = true)

        viewModelScope.launch {
            val photos = photoSaver.savePhotos()

            val calendar = Calendar.getInstance()
            calendar.timeInMillis = uiState.date
            Log.e("date is ", uiState.date.toString())

            val log = LogEntry(
                date = getIsoDate(uiState.date),
                place = uiState.place!!,
                photo1 = photos[0].name,
                photo2 = photos.getOrNull(1)?.name,
                photo3 = photos.getOrNull(2)?.name,
            )

            db.logDao().insert(log)
            uiState = uiState.copy(isSaved = true)
        }
    }
    // endregion
}

class AddLogViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val app = extras[APPLICATION_KEY] as PhotoLogApplication
        return AddLogViewModel(app, app.photoSaver) as T
    }
}