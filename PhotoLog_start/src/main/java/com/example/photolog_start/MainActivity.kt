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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.photolog_start.ui.theme.PhotoLogTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    lateinit var permissionManager: PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionManager = (application as PhotoLogApplication).permissions

        // TODO: Step 2. Register Data Access Audit Callback

        setContent {
            PhotoLogTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    val startNavigation =
                        if (permissionManager.hasAllPermissions) {
                            Screens.Home.route
                        } else {
                            Screens.Permissions.route
                        }

                    NavHost(navController = navController, startDestination = startNavigation) {
                        composable(Screens.Permissions.route) { PermissionScreen(navController) }
                        composable(Screens.Home.route) { HomeScreen(navController) }
                        composable(Screens.AddLog.route) { AddLogScreen(navController) }
                        composable(Screens.Camera.route) { CameraScreen(navController) }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            permissionManager.checkPermissions()
        }
    }

    // TODO: Step 1. Create Data Access Audit Listener Object

}

sealed class Screens(val route: String) {
    object Permissions : Screens("permissions")
    object Home : Screens("home")
    object AddLog : Screens("add_log")
    object Camera : Screens("camera")
}