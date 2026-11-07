package com.example.dynamicisland

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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
    
    // State Status
    var hasOverlay by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var serviceRunning by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    // State untuk Musik
    var showPlaylistSheet by remember { mutableStateOf(false) }
    var musicList by remember { mutableStateOf<List<Song>>(emptyList()) }
    val sheetState = rememberModalBottomSheetState()

    // Launcher Izin Membaca File Audio (Android 13+)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            musicList = MusicManager.scanMusic(context)
            if (musicList.isNotEmpty()) {
                showPlaylistSheet = true
            } else {
                Toast.makeText(context, "Tidak ada musik di HP", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Izin baca musik ditolak!", Toast.LENGTH_SHORT).show()
        }
    }

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
                    // MENU DROPDOWN (Selalu bisa diklik)
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
                                try {
                                    context.startActivity(Intent(context, SettingsActivity::class.java))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Halaman Setelan belum siap", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("🎨 Warna Island") },
                            onClick = {
                                showMenu = false
                                try {
                                    context.startActivity(Intent(context, ColorSettingsActivity::class.java))
                                } catch (e: Exception) {}
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("🎯 Notifikasi") },
                            onClick = {
                                showMenu = false
                                try {
                                    context.startActivity(Intent(context, NotificationSettingsActivity::class.java))
                                } catch (e: Exception) {}
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("ℹ️ Tentang") },
                            onClick = {
                                showMenu = false
                                try {
                                    context.startActivity(Intent(context, AboutActivity::class.java))
                                } catch (e: Exception) {}
                            }
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

            // ================= SIMULASI & MUSIK CARD =================
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("🎬 Simulasi & Musik", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))

                    // --- Tombol Simulasi Panggilan ---
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

                    // --- Tombol Simulasi Chat ---
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

                    // --- Tombol Simulasi Lagu Dummy ---
                    Button(
                        onClick = {
                            IslandState.updateMusic(
                                MusicInfo("Perfect", "Ed Sheeran", isPlaying = true)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)),
                        enabled = serviceRunning
                    ) { Text("🎵 Simulasi Lagu Dummy") }

                    Spacer(Modifier.height(8.dp))

                    // --- Tombol Navigasi Maps ---
                    Button(
                        onClick = {
                            IslandState.showNavigation(
                                NavigationInfo(
                                    instruction = "Belok Kanan",
                                    distance = "200 m",
                                    duration = "15 mnt",
                                    distanceTotal = "5,2 km",
                                    eta = "14:30",
                                    appName = "Google Maps",
                                    packageName = "com.google.android.apps.maps"
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                        enabled = serviceRunning
                    ) { Text("🗺️ Navigasi Maps") }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = Color.LightGray)
                    Spacer(Modifier.height(16.dp))

                    // --- Tombol Pemutar Musik dari HP ---
                    Text("🎸 Musik dari Memori HP", style = MaterialTheme.typography.labelLarge, color = Color.DarkGray)
                    Spacer(Modifier.height(8.dp))
                    
                    Button(
                        onClick = {
                            MusicManager.initPlayer(context)
                            
                            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                Manifest.permission.READ_MEDIA_AUDIO
                            } else {
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            }

                            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                                permissionLauncher.launch(permission)
                            } else {
                                musicList = MusicManager.scanMusic(context)
                                if (musicList.isNotEmpty()) {
                                    showPlaylistSheet = true
                                } else {
                                    Toast.makeText(context, "Tidak ada musik di HP", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B9D)),
                        enabled = serviceRunning
                    ) { 
                        Text("🎸 Pilih & Putar Musik dari HP") 
                    }

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
                        "• Atur warna di Setelan → Warna Island",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(Modifier.height(24.dp))

            // FOOTER
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                )
                Spacer(Modifier.height(12.dp))

                Text(
                    "Made by Aminsambung",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(4.dp))

                Text(
                    "Dynamic Island v1.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    // ================= BOTTOM SHEET PLAYLIST =================
    if (showPlaylistSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPlaylistSheet = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Pilih Lagu dari HP",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider()
                
                LazyColumn {
                    items(musicList.size) { index ->
                        val song = musicList[index]
                        ListItem(
                            headlineContent = { Text(song.title, maxLines = 1) },
                            supportingContent = { Text(song.artist) },
                            leadingContent = {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color(0xFFFF6B9D)
                                )
                            },
                            modifier = Modifier.clickable {
                                MusicManager.playSong(context, index)
                                showPlaylistSheet = false
                            }
                        )
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }

    // Timer untuk update status overlay
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
