/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.metrolist.music.R
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.viewmodels.ControlManagementViewModel
import kotlinx.coroutines.launch

private enum class ControlScreenState { SETUP, LOCKED, UNLOCKED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlManagementScreen(navController: NavController) {
    val viewModel: ControlManagementViewModel = hiltViewModel()
    val passwordHash by viewModel.passwordHash.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    var screenState by rememberSaveable {
        mutableStateOf(if (viewModel.isPasswordSet) ControlScreenState.LOCKED else ControlScreenState.SETUP)
    }

    LaunchedEffect(passwordHash) {
        if (screenState != ControlScreenState.UNLOCKED) {
            screenState = if (passwordHash == null) ControlScreenState.SETUP else ControlScreenState.LOCKED
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ניהול תוכן") },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
        ) {
            when (screenState) {
                ControlScreenState.SETUP -> PasswordSetupView(
                    onSetPassword = { password ->
                        coroutineScope.launch {
                            viewModel.setPassword(password)
                            screenState = ControlScreenState.UNLOCKED
                        }
                    },
                )
                ControlScreenState.LOCKED -> PasswordVerifyView(
                    onVerify = { input ->
                        if (viewModel.verifyPassword(input)) {
                            screenState = ControlScreenState.UNLOCKED
                            true
                        } else {
                            false
                        }
                    },
                )
                ControlScreenState.UNLOCKED -> ManagementDashboard(viewModel, navController)
            }
        }
    }
}

@Composable
private fun PasswordSetupView(onSetPassword: (String) -> Unit) {
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var errorMsg by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Icon(
            painter = painterResource(R.drawable.lock),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(text = "הגדרת סיסמה", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "הגדר סיסמה כדי להגן על מסך הניהול",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMsg = "" },
            label = { Text("סיסמה") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = errorMsg.isNotEmpty(),
        )
        OutlinedTextField(
            value = confirm,
            onValueChange = { confirm = it; errorMsg = "" },
            label = { Text("אמת סיסמה") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = errorMsg.isNotEmpty(),
            supportingText = { if (errorMsg.isNotEmpty()) Text(errorMsg) },
        )
        Button(
            onClick = {
                when {
                    password.length < 4 -> errorMsg = "הסיסמה חייבת להכיל לפחות 4 תווים"
                    password != confirm -> errorMsg = "הסיסמאות אינן תואמות"
                    else -> onSetPassword(password)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("שמור סיסמה")
        }
    }
}

@Composable
private fun PasswordVerifyView(onVerify: (String) -> Boolean) {
    var input by rememberSaveable { mutableStateOf("") }
    var failed by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Icon(
            painter = painterResource(R.drawable.lock),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(text = "הזן סיסמה", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = input,
            onValueChange = { input = it; failed = false },
            label = { Text("סיסמה") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = failed,
            supportingText = { if (failed) Text("סיסמה שגויה, נסה שוב") },
        )
        Button(
            onClick = {
                val success = onVerify(input)
                if (!success) {
                    failed = true
                    input = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("כניסה")
        }
    }
}

@Composable
private fun ManagementDashboard(
    viewModel: ControlManagementViewModel,
    navController: NavController,
) {
    val imagesEnabled by viewModel.imagesEnabled.collectAsStateWithLifecycle()
    val artistFilterEnabled by viewModel.artistFilterEnabled.collectAsStateWithLifecycle()
    val approvedArtists by viewModel.approvedArtists.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "הגדרות בקרה", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(4.dp))

        // Images toggle
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "הצגת תמונות", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "הצג או הסתר תמונות אמנים ואלבומים בכל האפליקציה",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = imagesEnabled,
                    onCheckedChange = { viewModel.setImagesEnabled(it) },
                )
            }
        }

        // Artist filter toggle
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "הגבלת גישה לאמנים", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "הצג רק מוזיקה מאמנים שאישרת",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = artistFilterEnabled,
                        onCheckedChange = { viewModel.setArtistFilterEnabled(it) },
                    )
                }
                if (artistFilterEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "אמנים מאושרים: ${approvedArtists.size}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        TextButton(
                            onClick = { navController.navigate("settings/control/artists") },
                        ) {
                            Text("נהל רשימה")
                        }
                    }
                }
            }
        }
    }
}

