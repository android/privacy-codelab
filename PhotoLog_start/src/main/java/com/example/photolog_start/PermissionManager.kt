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

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.CAMERA
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PermissionManager(private val context: Context) {
    companion object {
        val REQUIRED_PERMISSIONS = arrayOf(
            READ_EXTERNAL_STORAGE,
            CAMERA,
            ACCESS_FINE_LOCATION,
            ACCESS_COARSE_LOCATION
        )
    }

    data class State(
        val hasStorageAccess: Boolean,
        val hasCameraAccess: Boolean,
        val hasLocationAccess: Boolean
    ) {
        val hasAllAccess: Boolean
            get() = hasStorageAccess && hasCameraAccess && hasLocationAccess
    }

    private val _state = MutableStateFlow(
        State(
            hasStorageAccess = hasAccess(READ_EXTERNAL_STORAGE),
            hasCameraAccess = hasAccess(CAMERA),
            hasLocationAccess = hasAccess(listOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)),
        )
    )
    val state = _state.asStateFlow()
    val hasAllPermissions: Boolean
        get() = _state.value.hasAllAccess

    private fun hasAccess(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasAccess(permissions: List<String>): Boolean {
        return permissions.all(::hasAccess)
    }

    fun onPermissionChange(permissions: Map<String, Boolean>) {
        val hasLocationAccess = hasAccess(ACCESS_FINE_LOCATION) && hasAccess(ACCESS_COARSE_LOCATION)

        _state.value = State(
            hasStorageAccess = permissions[READ_EXTERNAL_STORAGE] ?: _state.value.hasStorageAccess,
            hasCameraAccess = permissions[CAMERA] ?: _state.value.hasCameraAccess,
            hasLocationAccess = hasLocationAccess
        )
    }

    suspend fun checkPermissions() {
        val newState = State(
            hasStorageAccess = hasAccess(READ_EXTERNAL_STORAGE),
            hasCameraAccess = hasAccess(CAMERA),
            hasLocationAccess = hasAccess(ACCESS_FINE_LOCATION) && hasAccess(ACCESS_COARSE_LOCATION)
        )

        _state.emit(newState)
    }

    fun createSettingsIntent(): Intent {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            data = Uri.fromParts("package", context.packageName, null)
        }

        return intent
    }
}