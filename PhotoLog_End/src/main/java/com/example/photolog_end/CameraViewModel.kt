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

package com.example.photolog_end

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CameraViewModel(
    application: Application,
    private val photoSaver: PhotoSaverRepository
) : AndroidViewModel(application) {

    private val context: Context
        get() = getApplication<PhotoGoodApplication>()
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    data class CameraState(
        val isTakingPicture: Boolean = false,
        val imageCapture: ImageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build(),
        val imageFile: File? = null,
        val captureError: ImageCaptureException? = null
    )

    var cameraState by mutableStateOf(CameraState())
        private set

    suspend fun getCameraProvider(): ProcessCameraProvider {
        return suspendCoroutine { continuation ->
            ProcessCameraProvider.getInstance(context).apply {
                addListener({ continuation.resume(get()) }, cameraExecutor)
            }
        }
    }

    fun takePicture() {
        viewModelScope.launch {
            cameraState = cameraState.copy(isTakingPicture = true)

            val savedFile = photoSaver.generatePhotoCacheFile()

            cameraState.imageCapture.takePicture(
                ImageCapture.OutputFileOptions.Builder(savedFile).build(),
                cameraExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        Log.i("TakePicture", "capture succeeded")

                        photoSaver.cacheCapturedPhoto(savedFile)
                        cameraState = cameraState.copy(imageFile = savedFile)
                    }

                    override fun onError(ex: ImageCaptureException) {
                        Log.e("TakePicture", "capture failed", ex)
                    }
                }
            )
        }
    }
}

class CameraViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val app = extras[APPLICATION_KEY] as PhotoGoodApplication
        return CameraViewModel(app, app.photoSaver) as T
    }
}