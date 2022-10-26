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

import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

data class Log(
    val date: String,
    val place: String,
    val photos: List<File>
) {
    val timeInMillis = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)!!.time

    fun toLogEntry(): LogEntry {
        return LogEntry(
            date = date,
            place = place,
            photo1 = photos[0].name,
            photo2 = photos.getOrNull(1)?.name,
            photo3 = photos.getOrNull(2)?.name
        )
    }

    companion object {
        fun fromLogEntry(logEntry: LogEntry, photoFolder: File): Log {
            return Log(
                date = logEntry.date,
                place = logEntry.place,
                photos = listOfNotNull(logEntry.photo1, logEntry.photo2, logEntry.photo3).map {
                    File(photoFolder, it)
                }
            )
        }
    }
}