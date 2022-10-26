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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory())
) {

    LaunchedEffect(Unit) { viewModel.loadLogs() }

    val state = viewModel.uiState
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarScrollState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SmallTopAppBar(
                title = { Text("My Logs", fontFamily = FontFamily.Serif) },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screens.AddLog.route) }) {
                Icon(Icons.Filled.Add, "Add log")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(8.dp)
        ) {
            if (!state.loading && state.logs.isEmpty()) {
                item {
                    EmptyLogMessage(Modifier.fillParentMaxSize())
                }
            }
            items(state.logs, key = { it.date }) { log ->
                LogCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItemPlacement(),
                    log = log,
                    formattedDate = viewModel.formatDateTime(log.timeInMillis),
                    onDelete = viewModel::delete
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun EmptyLogMessage(modifier: Modifier) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Hi there \uD83D\uDC4B",
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = FontFamily.Serif
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Create a log by clicking the ✚ icon below \uD83D\uDC47",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogCard(modifier: Modifier, log: Log, formattedDate: String, onDelete: (log: Log) -> Unit) {
    Card(modifier) {
        Row(Modifier.padding(8.dp, 0.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formattedDate,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall
            )
            IconButton(onClick = { onDelete(log) }) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete log"
                )
            }
        }
        Row(Modifier.padding(8.dp, 0.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Explore, null)
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(log.place)
        }
        PhotoGrid(Modifier.padding(16.dp), photos = log.photos)
    }
}
