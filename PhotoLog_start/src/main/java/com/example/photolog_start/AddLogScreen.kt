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
import android.Manifest.permission.CAMERA
import android.app.Activity
import android.app.DatePickerDialog
import android.net.Uri
import android.text.format.DateUtils
import android.text.format.DateUtils.FORMAT_ABBREV_ALL
import android.widget.DatePicker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun AddLogScreen(
    navController: NavHostController,
    viewModel: AddLogViewModel = viewModel(factory = AddLogViewModelFactory())
) {
    // region State initialization
    val state = viewModel.uiState
    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val internalPhotoPickerState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)

    fun canAddPhoto(callback: () -> Unit) {
        if (viewModel.canAddPhoto()) {
            callback()
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("You can't add more than $MAX_LOG_PHOTOS_LIMIT photos")
            }
        }
    }
    // endregion

    // TODO: Step 1. Register ActivityResult to request Camera permission

    // TODO: Step 3. Add explanation dialog for Camera permission

    // TODO: Step 5. Register ActivityResult to request Location permissions
    // TODO: Step 8. Change activity result to only request Coarse Location

    // TODO: Step 6. Add explanation dialog for Location permissions

    // TODO: Step 11. Register ActivityResult to launch the Photo Picker
    // region helper functions

    LaunchedEffect(Unit) {
        viewModel.refreshSavedPhotos()
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            navController.navigate(Screens.Home.route) {
                popUpTo(Screens.Home.route) {
                    inclusive = false
                }
            }
        }
    }

    val pickImage = rememberLauncherForActivityResult(
        PickMultipleVisualMedia(MAX_LOG_PHOTOS_LIMIT),
        viewModel::onPhotoPickerSelect
    )

    fun canSaveLog(callback: () -> Unit) {
        if (viewModel.isValid()) {
            callback()
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("You haven't completed all details")
            }
        }
    }
    // endregion

    // region UI - Bottom Sheet
    ModalBottomSheetLayout(
        sheetState = internalPhotoPickerState,
        sheetContent = {
            PhotoPicker(
                modifier = Modifier.fillMaxSize(),
                entries = state.localPickerPhotos,
                onSelect = { uri ->
                    coroutineScope.launch {
                        internalPhotoPickerState.hide()
                        viewModel.onLocalPhotoPickerSelect(uri)
                    }
                }
            )
        }
    )
    // endregion
    {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            // region UI - Top Bar & Action Button
            topBar = {
                SmallTopAppBar(
                    title = { Text("Add Log", fontFamily = FontFamily.Serif) },
                    navigationIcon = {
                        if (navController.previousBackStackEntry != null) {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    text = { Text("Save log") },
                    icon = {
                        if (state.isSaving) {
                            CircularProgressIndicator(Modifier.size(24.0.dp))
                        } else {
                            Icon(Icons.Filled.Check, null)
                        }
                    },
                    onClick = {
                        canSaveLog {
                            viewModel.createLog()
                        }
                    }
                )
            }
            // endregion
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center
            ) {
                // region Date
                ListItem(
                    headlineText = { Text("Date") },
                    trailingContent = { DatePicker(state.date, onChange = viewModel::onDateChange) }
                )
                Divider()
                // endregion

                // region Location
                ListItem(
                    headlineText = { Text("Location") },
                    trailingContent = {
                        // TODO: Step 7. Check, request, and explain Location permissions
                        LocationPicker(state.place) {
                            viewModel.fetchLocation()
                        }
                        // TODO: Step 9. Change location request to only request COARSE location.
                    }
                )
                Divider()
                // endregion

                // region Photos
                ListItem(
                    headlineText = { Text("Photos") },
                    trailingContent = {
                        Row {
                            // region Photo Picker
                            TextButton(onClick = {
                                canAddPhoto {
                                    viewModel.loadLocalPickerPictures()
                                    coroutineScope.launch {
                                        // TODO: Step 12. Replace the line below showing our internal
                                        //  photo picking UI and launch the Android Photo Picker instead
                                        internalPhotoPickerState.show()
                                    }
                                }
                            }) {
                                Icon(Icons.Filled.PhotoLibrary, null)
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                Text("Add photo")
                            }
                            // endregion

                            // region Camera
                            IconButton(onClick = {
                                // TODO: Step 2. Check & request for Camera permission before navigating to the camera screen.
                                canAddPhoto {
                                    navController.navigate(Screens.Camera.route)
                                }
                            })
                            {
                                Icon(Icons.Filled.AddAPhoto, null)
                            }
                            // endregion
                        }
                    }
                )
                // endregion

                PhotoGrid(
                    modifier = Modifier.padding(16.dp),
                    photos = state.savedPhotos,
                    onRemove = { photo -> viewModel.onPhotoRemoved(photo) }
                )
            }
        }
    }
}

@Composable
fun DatePicker(timeInMillis: Long, onChange: (time: Long) -> Unit) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timeInMillis

    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            calendar.set(year, month, dayOfMonth)
            onChange(calendar.timeInMillis)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    TextButton(onClick = { datePickerDialog.show() }) {
        Icon(Icons.Filled.CalendarToday, null)
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(DateUtils.formatDateTime(context, timeInMillis, FORMAT_ABBREV_ALL))
    }
}

@Composable
fun LocationPicker(address: String?, fetchLocation: () -> Unit) {
    TextButton(onClick = { fetchLocation() }) {
        Icon(Icons.Filled.Explore, null)
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(address ?: "Get location")
    }
}

@Composable
fun PhotoPicker(modifier: Modifier = Modifier, entries: List<Uri>, onSelect: (uri: Uri) -> Unit) {
    LazyVerticalGrid(modifier = modifier, columns = GridCells.Fixed(3)) {
        items(entries) { uri ->
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .aspectRatio(1f)
                    .clickable { onSelect(uri) }
            )
        }
    }
}

@Composable
fun CameraExplanationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Camera access") },
        text = { Text("PhotoLog would like access to the camera to be able take picture when creating a log") },
        icon = {
            Icon(
                Icons.Filled.Camera,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.surfaceTint
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Continue")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
}

@Composable
fun LocationExplanationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Location access") },
        text = { Text("PhotoLog would like access to your location to save it when creating a log") },
        icon = {
            Icon(
                Icons.Filled.Explore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.surfaceTint
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Continue")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
}