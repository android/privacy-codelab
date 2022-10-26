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

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PhotoSaverRepository(context: Context, private val contentResolver: ContentResolver) {

    private val _photos = mutableListOf<File>()

    fun getPhotos() = _photos.toList()
    fun isEmpty() = _photos.isEmpty()
    fun canAddPhoto() = _photos.size < MAX_LOG_PHOTOS_LIMIT

    private val cacheFolder = File(context.cacheDir, "photos").also { it.mkdir() }
    val photoFolder = File(context.filesDir, "photos").also { it.mkdir() }

    private fun generateFileName() = "${System.currentTimeMillis()}.jpg"
    private fun generatePhotoLogFile() = File(photoFolder, generateFileName())
    fun generatePhotoCacheFile() = File(cacheFolder, generateFileName())

    fun cacheCapturedPhoto(photo: File) {
        if (_photos.size + 1 > MAX_LOG_PHOTOS_LIMIT) {
            return
        }

        _photos += photo
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun cacheFromUri(uri: Uri) {
        withContext(Dispatchers.IO) {
            if (_photos.size + 1 > MAX_LOG_PHOTOS_LIMIT) {
                return@withContext
            }

            contentResolver.openInputStream(uri)?.use { input ->
                val cachedPhoto = generatePhotoCacheFile()

                cachedPhoto.outputStream().use { output ->
                    input.copyTo(output)
                    _photos += cachedPhoto
                }
            }
        }
    }

    suspend fun cacheFromUris(uris: List<Uri>) {
        uris.forEach {
            cacheFromUri(it)
        }
    }

    suspend fun removeFile(photo: File) {
        withContext(Dispatchers.IO) {
            photo.delete()
            _photos -= photo
        }
    }

    suspend fun savePhotos(): List<File> {
        return withContext(Dispatchers.IO) {
            val savedPhotos = _photos.map { it.copyTo(generatePhotoLogFile()) }

            _photos.forEach { it.delete() }
            _photos.clear()

            savedPhotos
        }
    }
}