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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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

@Composable
fun MainScreen() {
    val context = LocalContext.current
    var hasOverlay by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var serviceRunning by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Text("🌴 Dynamic Island", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Overlay pill ala iPhone 14 Pro",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(Modifier.height(32.dp))

        // ================= SETUP CARD =================
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("⚙️ Setup", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))

                StatusRow("Izin Overlay", hasOverlay)
                Spacer(Modifier.height(8.dp))
                StatusRow("Service Aktif", serviceRunning)

                Spacer(Modifier.height(16.dp))

                // 1. Izin Overlay
                Button(
                    onClick = {
                        if (!Settings.canDrawOverlays(context)) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !hasOverlay
                ) {
                    Text(if (hasOverlay) "✅ Overlay diizinkan" else "1. Izinkan Overlay")
                }

                Spacer(Modifier.height(8.dp))

                // 2. Izin Notification Access
                Button(
                    onClick = {
                        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("2. Izinkan Akses Notifikasi (WA & lagu)")
                }

                Spacer(Modifier.height(8.dp))

                // 3. Aktifkan Service
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
                ) {
                    Text("3. Aktifkan Dynamic Island")
                }

                Spacer(Modifier.height(8.dp))

                // Stop Service
                OutlinedButton(
                    onClick = {
                        context.stopService(Intent(context, DynamicIslandService::class.java))
                        serviceRunning = false
                        IslandState.reset()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = serviceRunning
                ) {
                    Text("Matikan Service")
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ================= SIMULASI CARD =================
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("🎬 Simulasi", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))

                // Simulasi Panggilan
                Button(
                    onClick = {
                        IslandState.showIncomingCall(
                            CallInfo("Budi Santoso", "+62 812-3456-7890", "B")
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF30D158)
                    ),
                    enabled = serviceRunning
                ) {
                    Text("📞 Panggilan Masuk")
                }

                Spacer(Modifier.height(8.dp))

                // Simulasi Chat WA
                Button(
                    onClick = {
                        IslandState.showChat(
                            ChatInfo(
                                senderName = "Ibu",
                                message = "Sudah makan belum nak?",
                                avatarInitial = "I",
                                packageName = "com.whatsapp"
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF25D366)
                    ),
                    enabled = serviceRunning
                ) {
                    Text("💬 Pesan WhatsApp")
                }

                Spacer(Modifier.height(8.dp))

                // Simulasi Musik
                Button(
                    onClick = {
                        IslandState.updateMusic(
                            MusicInfo(
                                title = "Perfect",
                                artist = "Ed Sheeran",
                                isPlaying = true
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1DB954)
                    ),
                    enabled = serviceRunning
                ) {
                    Text("🎵 Simulasi Lagu")
                }

                Spacer(Modifier.height(8.dp))

                // Reset
                OutlinedButton(
                    onClick = { IslandState.reset() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = serviceRunning
                ) {
                    Text("↩️ Reset ke Idle")
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Tips:\n" +
                "1. Aktifkan izin Overlay & Notifikasi\n" +
                "2. Aktifkan service\n" +
                "3. Play lagu di Spotify / terima WA\n" +
                "4. Tap island untuk kecilkan 🎵",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

        Spacer(Modifier.height(40.dp))
    }

    // Auto-refresh status overlay
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
