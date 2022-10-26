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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "logs")
data class LogEntry(
    @PrimaryKey val date: String, // date format: yyyy-MM-dd
    @ColumnInfo(name = "place") val place: String,
    @ColumnInfo(name = "photo1_name") val photo1: String,
    @ColumnInfo(name = "photo2_name") val photo2: String? = null,
    @ColumnInfo(name = "photo3_name") val photo3: String? = null
)

const val MAX_LOG_PHOTOS_LIMIT = 3