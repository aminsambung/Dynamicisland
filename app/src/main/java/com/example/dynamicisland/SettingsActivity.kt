package com.example.dynamicisland

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    SettingsMenuScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMenuScreen() {
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚙️ Setelan", fontWeight = FontWeight.Bold) },
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
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))

            SectionHeader("🎨 Tampilan")

            SettingsItem(
                emoji = "🎨",
                title = "Warna Island",
                subtitle = "Pilih warna / gradient island",
                onClick = {
                    context.startActivity(
                        Intent(context, ColorSettingsActivity::class.java)
                    )
                }
            )

            Spacer(Modifier.height(8.dp))

            SectionHeader("🔔 Notifikasi")

            SettingsItem(
                emoji = "🎯",
                title = "Notifikasi per App",
                subtitle = "Pilih app yang muncul di island",
                onClick = {
                    context.startActivity(
                        Intent(context, NotificationSettingsActivity::class.java)
                    )
                }
            )

            Spacer(Modifier.height(8.dp))

            SectionHeader("ℹ️ Info")

            SettingsItem(
                emoji = "ℹ️",
                title = "Tentang",
                subtitle = "Info developer & versi",
                onClick = {
                    context.startActivity(
                        Intent(context, AboutActivity::class.java)
                    )
                }
            )

            SettingsItem(
                emoji = "🔄",
                title = "Reset Semua Setting",
                subtitle = "Kembalikan ke pengaturan awal",
                color = Color(0xFFFF3B30),
                onClick = {
                    IslandPreferences.resetAll(context)
                    IslandState.islandColor.value = IslandPreferences.DEFAULT_COLOR
                    IslandState.islandColorBottom.value = IslandPreferences.DEFAULT_COLOR
                    IslandState.useGradient.value = false
                    IslandState.glassAlpha.value = IslandPreferences.DEFAULT_GLASS_ALPHA
                    IslandState.borderAlpha.value = IslandPreferences.DEFAULT_BORDER_ALPHA
                }
            )

            Spacer(Modifier.height(40.dp))

            Text(
                "Made by Aminsambung",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsItem(
    emoji: String,
    title: String,
    subtitle: String,
    color: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 22.sp)
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (color != Color.Unspecified) color else Color.Unspecified
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.Gray
        )
    }
}
