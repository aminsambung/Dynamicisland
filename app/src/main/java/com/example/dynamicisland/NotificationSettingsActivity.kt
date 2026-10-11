package com.example.dynamicisland

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class NotificationSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    NotificationSettingsScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen() {
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    // State toggle per app
    val appStates = remember {
        mutableStateMapOf<String, Boolean>().apply {
            NotificationPreferences.AVAILABLE_APPS.forEach { app ->
                put(app.packageName, NotificationPreferences.isAppEnabled(context, app.packageName))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🎯 Notifikasi Island", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { activity?.finish() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Info card
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "💡 Info",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Aktifkan app yang ingin muncul di Dynamic Island. " +
                                "Panggilan, alarm, dan navigasi selalu muncul.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            // Tombol bulk action
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        NotificationPreferences.AVAILABLE_APPS.forEach { app ->
                            NotificationPreferences.setAppEnabled(context, app.packageName, true)
                            appStates[app.packageName] = true
                        }
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text("Aktif Semua", fontSize = 10.sp, maxLines = 1)
                }
                OutlinedButton(
                    onClick = {
                        NotificationPreferences.AVAILABLE_APPS.forEach { app ->
                            NotificationPreferences.setAppEnabled(context, app.packageName, false)
                            appStates[app.packageName] = false
                        }
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text("Matikan", fontSize = 10.sp, maxLines = 1)
                }
                OutlinedButton(
                    onClick = {
                        NotificationPreferences.resetAll(context)
                        NotificationPreferences.AVAILABLE_APPS.forEach { app ->
                            val enabled = NotificationPreferences.isAppEnabled(context, app.packageName)
                            appStates[app.packageName] = enabled
                        }
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text("Reset", fontSize = 10.sp, maxLines = 1)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Daftar app
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(NotificationPreferences.AVAILABLE_APPS) { app ->
                    val enabled = appStates[app.packageName] ?: false

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (enabled)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(app.emoji, fontSize = 24.sp,
                                modifier = Modifier.padding(end = 12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    app.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    app.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            }

                            Switch(
                                checked = enabled,
                                onCheckedChange = { checked ->
                                    appStates[app.packageName] = checked
                                    NotificationPreferences.setAppEnabled(
                                        context, app.packageName, checked
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
