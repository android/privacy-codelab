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

import android.app.AppOpsManager
import android.app.AppOpsManager.OPSTR_CAMERA
import android.app.AppOpsManager.OPSTR_COARSE_LOCATION
import android.app.AppOpsManager.OPSTR_FINE_LOCATION
import android.app.AsyncNotedAppOp
import android.app.SyncNotedAppOp
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.photolog_end.ui.theme.PhotoGoodTheme
import kotlinx.coroutines.launch
import android.util.Log

class MainActivity : ComponentActivity() {
    lateinit var permissionManager: PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionManager = (application as PhotoGoodApplication).permissions

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val appOpsManager = getSystemService(AppOpsManager::class.java) as AppOpsManager
            appOpsManager.setOnOpNotedCallback(mainExecutor, DataAccessAuditListener)
        }

        setContent {
            PhotoGoodTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    val startNavigation = Screens.Home.route

                    NavHost(navController = navController, startDestination = startNavigation) {
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

    @RequiresApi(Build.VERSION_CODES.R)
    object DataAccessAuditListener : AppOpsManager.OnOpNotedCallback() {
        // note: we are just logging to console for this codelab but you can also integrate
        // other logging and reporting systems here to track your app's private data access.
        override fun onNoted(op: SyncNotedAppOp) {
            Log.d("DataAccessAuditListener","Sync Private Data Accessed: ${op.op}")
        }

        override fun onSelfNoted(op: SyncNotedAppOp) {
            Log.d("DataAccessAuditListener","Self Private Data accessed: ${op.op}")
        }

        override fun onAsyncNoted(asyncNotedAppOp: AsyncNotedAppOp) {
            var emoji = when (asyncNotedAppOp.op) {
                OPSTR_COARSE_LOCATION -> "\uD83D\uDDFA"
                OPSTR_CAMERA -> "\uD83D\uDCF8"
                else -> "?"
            }

            Log.d("DataAccessAuditListener", "Async Private Data ($emoji) Accessed: ${asyncNotedAppOp.op}")
        }
    }
}

sealed class Screens(val route: String) {
    object Permissions : Screens("permissions")
    object Home : Screens("home")
    object AddLog : Screens("add_log")
    object Camera : Screens("camera")
}