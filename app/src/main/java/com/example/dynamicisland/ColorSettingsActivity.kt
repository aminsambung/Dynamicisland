package com.example.dynamicisland

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class ColorSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    ColorSettingsScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorSettingsScreen() {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    var isGradient by remember { mutableStateOf(IslandState.useGradient.value) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🎨 Warna Island", fontWeight = FontWeight.Bold) },
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
                .padding(16.dp)
        ) {
            // ============ PREVIEW ============
            Text(
                "Preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (isGradient) {
                                listOf(
                                    Color(IslandState.islandColor.value).copy(alpha = IslandState.glassAlpha.value),
                                    Color(IslandState.islandColorBottom.value).copy(alpha = IslandState.glassAlpha.value * 0.85f)
                                )
                            } else {
                                listOf(
                                    Color(IslandState.islandColor.value).copy(alpha = IslandState.glassAlpha.value),
                                    Color(IslandState.islandColor.value).copy(alpha = IslandState.glassAlpha.value * 0.85f)
                                )
                            }
                        )
                    )
                    .border(
                        width = 0.5.dp,
                        color = Color.White.copy(alpha = IslandState.borderAlpha.value),
                        shape = RoundedCornerShape(40.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "🎵 Contoh Preview",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(24.dp))

            // ============ SLIDER TRANSPARANSI & BORDER ============
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    // Slider Transparansi
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "🎚️ Transparansi",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "${(IslandState.glassAlpha.value * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = IslandState.glassAlpha.value,
                        onValueChange = { newValue ->
                            IslandState.glassAlpha.value = newValue
                            IslandPreferences.setGlassAlpha(context, newValue)
                        },
                        valueRange = 0.1f..1f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    // Slider Border
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "🔲 Ketebalan Border",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "${(IslandState.borderAlpha.value * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = IslandState.borderAlpha.value,
                        onValueChange = { newValue ->
                            IslandState.borderAlpha.value = newValue
                            IslandPreferences.setBorderAlpha(context, newValue)
                        },
                        valueRange = 0f..0.5f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ============ TOGGLE GRADIENT ============
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "🌈 Mode Gradient",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (isGradient) "2 warna (atas & bawah)"
                            else "1 warna solid",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = isGradient,
                        onCheckedChange = { checked ->
                            isGradient = checked
                            IslandState.useGradient.value = checked
                            IslandPreferences.setUseGradient(context, checked)
                        }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ============ PRESET GRADIENT ============
            if (isGradient) {
                Text(
                    "🌈 Preset Gradient",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Kombinasi warna siap pakai",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    IslandPreferences.GRADIENT_PRESETS.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { preset ->
                                GradientButton(
                                    preset = preset,
                                    isSelected = IslandState.islandColor.value == preset.colorTop &&
                                            IslandState.islandColorBottom.value == preset.colorBottom,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        IslandState.islandColor.value = preset.colorTop
                                        IslandState.islandColorBottom.value = preset.colorBottom
                                        IslandPreferences.setIslandColor(context, preset.colorTop)
                                        IslandPreferences.setIslandColorBottom(context, preset.colorBottom)
                                    }
                                )
                            }
                            repeat(2 - row.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Divider()
                Spacer(Modifier.height(16.dp))
            }

            // ============ WARNA ATAS ============
            Text(
                if (isGradient) "🎨 Warna Atas" else "🎨 Pilih Warna",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                IslandPreferences.COLOR_PRESETS.chunked(4).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { preset ->
                            ColorButton(
                                preset = preset,
                                isSelected = IslandState.islandColor.value == preset.color,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    IslandState.islandColor.value = preset.color
                                    IslandPreferences.setIslandColor(context, preset.color)
                                }
                            )
                        }
                        repeat(4 - row.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            // ============ WARNA BAWAH ============
            if (isGradient) {
                Spacer(Modifier.height(24.dp))
                Text(
                    "🎨 Warna Bawah",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    IslandPreferences.COLOR_PRESETS.chunked(4).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { preset ->
                                ColorButton(
                                    preset = preset,
                                    isSelected = IslandState.islandColorBottom.value == preset.color,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        IslandState.islandColorBottom.value = preset.color
                                        IslandPreferences.setIslandColorBottom(context, preset.color)
                                    }
                                )
                            }
                            repeat(4 - row.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ============ RESET ============
            OutlinedButton(
                onClick = {
                    IslandState.islandColor.value = IslandPreferences.DEFAULT_COLOR
                    IslandState.islandColorBottom.value = IslandPreferences.DEFAULT_COLOR
                    isGradient = false
                    IslandState.useGradient.value = false
                    IslandState.glassAlpha.value = IslandPreferences.DEFAULT_GLASS_ALPHA
                    IslandState.borderAlpha.value = IslandPreferences.DEFAULT_BORDER_ALPHA
                    IslandPreferences.setIslandColor(context, IslandPreferences.DEFAULT_COLOR)
                    IslandPreferences.setIslandColorBottom(context, IslandPreferences.DEFAULT_COLOR)
                    IslandPreferences.setUseGradient(context, false)
                    IslandPreferences.setGlassAlpha(context, IslandPreferences.DEFAULT_GLASS_ALPHA)
                    IslandPreferences.setBorderAlpha(context, IslandPreferences.DEFAULT_BORDER_ALPHA)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("↩️ Reset ke Default")
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun GradientButton(
    preset: IslandPreferences.GradientPreset,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(preset.colorTop),
                            Color(preset.colorBottom)
                        )
                    )
                )
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Text("✓", color = Color.White, fontSize = 24.sp,
                    fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            preset.name,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 10.sp,
            color = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                Color.Gray,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ColorButton(
    preset: IslandPreferences.ColorPreset,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(preset.color))
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Text("✓", color = Color.White, fontSize = 20.sp,
                    fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            preset.name,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 9.sp,
            color = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                Color.Gray,
            maxLines = 1,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}
