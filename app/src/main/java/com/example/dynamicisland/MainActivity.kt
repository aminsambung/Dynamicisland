package com.example.dynamicisland

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    var hasOverlay by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var serviceRunning by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "🌴 Dynamic Island",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // Tombol 3-titik
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("⚙️ Setelan") },
                            onClick = {
                                showMenu = false
                                context.startActivity(
                                    Intent(context, SettingsActivity::class.java)
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("🎨 Warna Island") },
                            onClick = {
                                showMenu = false
                                context.startActivity(
                                    Intent(context, ColorSettingsActivity::class.java)
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("🎯 Notifikasi") },
                            onClick = {
                                showMenu = false
                                context.startActivity(
                                    Intent(context, NotificationSettingsActivity::class.java)
                                )
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("ℹ️ Tentang") },
                            onClick = { showMenu = false }
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Overlay pill ala iPhone 14 Pro",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(Modifier.height(24.dp))

            // ================= SETUP CARD =================
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("⚙️ Setup", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))

                    StatusRow("Izin Overlay", hasOverlay)
                    Spacer(Modifier.height(8.dp))
                    StatusRow("Service Aktif", serviceRunning)

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (!Settings.canDrawOverlays(context)) {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !hasOverlay
                    ) {
                        Text(if (hasOverlay) "✅ Overlay diizinkan" else "1. Izinkan Overlay")
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = {
                            context.startActivity(
                                Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("2. Izinkan Akses Notifikasi") }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val intent = Intent(context, DynamicIslandService::class.java)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(intent)
                            } else {
                                context.startService(intent)
                            }
                            serviceRunning = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = hasOverlay && !serviceRunning
                    ) { Text("3. Aktifkan Dynamic Island") }

                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            context.stopService(Intent(context, DynamicIslandService::class.java))
                            serviceRunning = false
                            IslandState.reset()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = serviceRunning
                    ) { Text("Matikan Service") }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ================= SIMULASI CARD =================
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("🎬 Simulasi", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            IslandState.showIncomingCall(
                                CallInfo("Budi Santoso", "+62 812-3456-7890", "B", "com.whatsapp")
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30D158)),
                        enabled = serviceRunning
                    ) { Text("📞 Panggilan Masuk") }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = {
                            IslandState.showChat(
                                ChatInfo("Ibu", "Sudah makan belum nak?", "I", "com.whatsapp")
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        enabled = serviceRunning
                    ) { Text("💬 Pesan WhatsApp") }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = {
                            IslandState.updateMusic(
                                MusicInfo("Perfect", "Ed Sheeran", isPlaying = true)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)),
                        enabled = serviceRunning
                    ) { Text("🎵 Simulasi Lagu") }

                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { IslandState.reset() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = serviceRunning
                    ) { Text("↩️ Reset ke Idle") }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "Tips:\n" +
                        "• Tap ⋮ (3-titik) kanan atas untuk Setelan\n" +
                        "• Atur warna di Setelan → Warna Island\n" +
                        "• Pilih notif app di Setelan → Notifikasi",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(Modifier.height(40.dp))
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            hasOverlay = Settings.canDrawOverlays(context)
            delay(1000)
        }
    }
}

@Composable
private fun StatusRow(label: String, ok: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(if (ok) "🟢" else "🔴")
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
