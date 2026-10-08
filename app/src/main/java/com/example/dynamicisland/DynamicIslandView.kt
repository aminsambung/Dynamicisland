package com.example.dynamicisland

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs

// ============================================
// WARNA TEMA — GLASSMORPHISM (Opsi B)
// ============================================
// Gradient semi-transparan:
// - Atas: 80% opacity (lebih terang)
// - Bawah: 67% opacity (lebih tembus)
private val IslandGlassTop = Color(0xCC1A1A1A)
private val IslandGlassBottom = Color(0xAA0A0A0A)
private val IslandBorderColor = Color(0x14FFFFFF)   // putih 8% untuk border tipis

// Warna aksen
private val GreenAccept = Color(0xFF30D158)
private val RedDecline = Color(0xFFFF3B30)
private val SpotifyGreen = Color(0xFF1DB954)
private val WhatsAppGreen = Color(0xFF25D366)
private val CameraDotGreen = Color(0xFF00E676)
private val AvatarBg = Color(0xFF2C2C2E)

// ============================================
// MAIN UI
// ============================================
@Composable
fun DynamicIslandUI() {
    val context = LocalContext.current
    val mode = IslandState.mode.value
    val call = IslandState.callInfo.value
    val isCollapsed = IslandState.shouldShowCollapsed()

    val effectiveMode = if (isCollapsed) IslandMode.IDLE else mode

    val targetWidth = when (effectiveMode) {
        IslandMode.IDLE -> if (isCollapsed && !IslandState.musicInfo.value.isEmpty) 190.dp else 130.dp
        IslandMode.MUSIC -> 340.dp
        IslandMode.CALL_RINGING -> 360.dp
        IslandMode.CALL_ACTIVE -> 240.dp
        IslandMode.CHAT -> 340.dp
    }
    val targetHeight = when (effectiveMode) {
        IslandMode.IDLE -> 36.dp
        IslandMode.MUSIC -> 72.dp
        IslandMode.CALL_RINGING -> 100.dp
        IslandMode.CALL_ACTIVE -> 40.dp
        IslandMode.CHAT -> 72.dp
    }

    val width by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 350f),
        label = "width"
    )
    val height by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 350f),
        label = "height"
    )

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(percent = 50))
            // ⬇️ GLASSMORPHISM: gradient semi-transparan
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(IslandGlassTop, IslandGlassBottom)
                )
            )
            // ⬇️ Border tipis biar keliatan "mengambang"
            .border(
                width = 0.5.dp,
                color = IslandBorderColor,
                shape = RoundedCornerShape(percent = 50)
            )
            // ============ TAP / DOUBLE TAP / LONG PRESS ============
            .pointerInput(mode, isCollapsed) {
                detectTapGestures(
                    onTap = {
                        when (mode) {
                            IslandMode.MUSIC -> {
                                if (isCollapsed) {
                                    IslandState.toggleCollapse()
                                } else {
                                    MediaControlBridge.playPause()
                                }
                            }
                            IslandMode.CHAT -> {
                                openApp(context, IslandState.chatInfo.value.packageName)
                                IslandState.dismissChat()
                            }
                            IslandMode.CALL_RINGING -> {
                                openApp(context, IslandState.callInfo.value.packageName)
                                IslandState.dismissCall()
                            }
                            IslandMode.CALL_ACTIVE -> {
                                openApp(context, IslandState.callInfo.value.packageName)
                            }
                            IslandMode.IDLE -> {
                                if (!IslandState.musicInfo.value.isEmpty) {
                                    IslandState.showMusic()
                                }
                            }
                        }
                    },
                    onDoubleTap = {
                        when (mode) {
                            IslandMode.MUSIC -> IslandState.toggleCollapse()
                            IslandMode.CHAT -> IslandState.dismissChat()
                            else -> {}
                        }
                    },
                    onLongPress = {
                        when (mode) {
                            IslandMode.MUSIC -> openMusicApp(context)
                            IslandMode.CALL_ACTIVE -> openDialer(context)
                            else -> {}
                        }
                    }
                )
            }
            // ============ SWIPE ============
            .pointerInput(mode) {
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onDragEnd = {
                        when (mode) {
                            IslandMode.MUSIC -> {
                                when {
                                    totalDrag < -80f -> MediaControlBridge.skipPrevious()
                                    totalDrag > 80f -> MediaControlBridge.skipNext()
                                }
                            }
                            IslandMode.CHAT -> {
                                if (abs(totalDrag) > 80f) IslandState.dismissChat()
                            }
                            IslandMode.CALL_RINGING -> {
                                if (abs(totalDrag) > 80f) IslandState.dismissCall()
                            }
                            else -> {}
                        }
                        totalDrag = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        totalDrag += dragAmount
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        when (effectiveMode) {
            IslandMode.IDLE -> {
                if (isCollapsed && !IslandState.musicInfo.value.isEmpty) {
                    MiniMusicContent()
                } else {
                    IdleContent()
                }
            }
            IslandMode.MUSIC -> MusicContent()
            IslandMode.CALL_RINGING -> CallRingingContent(call)
            IslandMode.CALL_ACTIVE -> CallActiveContent(call)
            IslandMode.CHAT -> ChatContent()
        }
    }
}

// ============================================
// IDLE
// ============================================
@Composable
private fun IdleContent() {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(CameraDotGreen))
        Text("• • •", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
    }
}

// ============================================
// MINI MUSIC (collapsed)
// ============================================
@Composable
private fun MiniMusicContent() {
    val music = IslandState.musicInfo.value
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        RotatingAlbumArt(
            size = 20.dp,
            iconSize = 11.dp,
            isPlaying = music.isPlaying,
            albumArt = music.albumArt
        )

        if (music.isPlaying) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val heights = listOf(5.dp, 9.dp, 5.dp)
                repeat(3) { i ->
                    Box(
                        Modifier
                            .padding(horizontal = 1.dp)
                            .width(2.dp)
                            .height(heights[i])
                            .clip(RoundedCornerShape(1.dp))
                            .background(SpotifyGreen)
                    )
                }
            }
        }
    }
}

// ============================================
// MUSIC (expanded)
// ============================================
@Composable
private fun MusicContent() {
    val music = IslandState.musicInfo.value
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RotatingAlbumArt(
            size = 46.dp,
            iconSize = 24.dp,
            isPlaying = music.isPlaying,
            albumArt = music.albumArt
        )

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                music.title.ifEmpty { "Tidak ada lagu" },
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                music.artist.ifEmpty { "—" },
                color = Color.Gray,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))

        if (music.isPlaying) {
            EqualizerBars()
        }
    }
}

// ============================================
// ROTATING ALBUM ART
// Berputar saat playing, DIAM saat pause
// ============================================
@Composable
private fun RotatingAlbumArt(
    size: Dp,
    iconSize: Dp,
    isPlaying: Boolean,
    albumArt: Bitmap?
) {
    val infinite = rememberInfiniteTransition(label = "rotate")
    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 8000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val currentRotation = if (isPlaying) rotation else 0f

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer { rotationZ = currentRotation }
            .clip(CircleShape)
            .background(SpotifyGreen),
        contentAlignment = Alignment.Center
    ) {
        if (albumArt != null && !albumArt.isRecycled) {
            Image(
                bitmap = albumArt.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        } else {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

// ============================================
// EQUALIZER BARS
// ============================================
@Composable
private fun EqualizerBars() {
    val infinite = rememberInfiniteTransition(label = "eq")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            val h = (6 + ((phase * 10 + i * 3) % 10)).dp
            Box(
                Modifier
                    .padding(horizontal = 1.dp)
                    .width(3.dp)
                    .height(h)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SpotifyGreen)
            )
        }
    }
}

// ============================================
// CHAT (WhatsApp dll)
// ============================================
@Composable
private fun ChatContent() {
    val chat = IslandState.chatInfo.value
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(46.dp).clip(CircleShape).background(WhatsAppGreen),
            contentAlignment = Alignment.Center
        ) {
            Text(
                chat.avatarInitial.ifEmpty { "?" },
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                chat.senderName.ifEmpty { "Pesan Baru" },
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                chat.message,
                color = Color.Gray,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Chat,
                contentDescription = null,
                tint = WhatsAppGreen,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "›",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ============================================
// CALL RINGING
// ============================================
@Composable
private fun CallRingingContent(call: CallInfo) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(56.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(GreenAccept.copy(alpha = 0.25f))
            )
            Box(
                Modifier.size(46.dp).clip(CircleShape).background(AvatarBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    call.avatarInitial,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                call.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Panggilan masuk",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    maxLines = 1
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "›",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        CallButton(
            icon = Icons.Default.CallEnd,
            bg = RedDecline,
            onClick = { IslandState.endCall() }
        )
        Spacer(Modifier.width(8.dp))
        CallButton(
            icon = Icons.Default.Call,
            bg = GreenAccept,
            onClick = { IslandState.acceptCall() }
        )
    }
}

// ============================================
// CALL ACTIVE
// ============================================
@Composable
private fun CallActiveContent(call: CallInfo) {
    var seconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            seconds++
        }
    }
    val timeText = "%02d:%02d".format(seconds / 60, seconds % 60)

    val infinite = rememberInfiniteTransition(label = "wave")
    val wave by infinite.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave"
    )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(26.dp).clip(CircleShape).background(GreenAccept),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Call,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                call.name,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(timeText, color = GreenAccept, fontSize = 10.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(4) { i ->
                val h = (6 + (i * 3)) * wave
                Box(
                    Modifier
                        .padding(horizontal = 1.dp)
                        .width(3.dp)
                        .height(h.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(GreenAccept)
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        CallButton(
            icon = Icons.Default.CallEnd,
            bg = RedDecline,
            size = 32.dp,
            iconSize = 16.dp,
            onClick = { IslandState.endCall() }
        )
    }
}

// ============================================
// REUSABLE — Tombol Call
// ============================================
@Composable
private fun CallButton(
    icon: ImageVector,
    bg: Color,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}

// ============================================
// HELPER — Buka app berdasarkan package name
// ============================================
private fun openApp(context: Context, packageName: String) {
    if (packageName.isEmpty()) return

    context.packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        )
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("DynamicIsland", "openApp failed: $packageName", e)
        }
    } ?: run {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}

// ============================================
// HELPER — Buka app pemutar musik
// ============================================
private fun openMusicApp(context: Context) {
    val packageName = IslandState.musicInfo.value.packageName

    if (packageName.isNotEmpty()) {
        context.packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
            intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            )
            try {
                context.startActivity(intent)
                return
            } catch (_: Exception) {}
        }
    }

    val fallbackPackages = listOf(
        "com.spotify.music",
        "com.google.android.apps.youtube.music",
        "com.apple.android.music",
        "in.krosbits.musicolet",
        "com.maxmpz.audioplayer",
        "com.aimp.player",
        "org.videolan.vlc",
        "com.kodarkooperativet.blackplayerfree",
        "code.name.monkey.retromusic",
        "com.miui.player",
        "com.samsung.android.music",
        "com.sec.android.app.music",
        "com.oppo.music",
        "com.coloros.music",
        "com.vivo.music",
        "com.google.android.music",
        "com.android.music"
    )

    for (pkg in fallbackPackages) {
        context.packageManager.getLaunchIntentForPackage(pkg)?.let { intent ->
            intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            )
            try {
                context.startActivity(intent)
                return
            } catch (_: Exception) {}
        }
    }

    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("market://search?q=music+player")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}

// ============================================
// HELPER — Buka dialer
// ============================================
private fun openDialer(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}

// ============================================
// PREVIEWS
// ============================================
@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A, widthDp = 400, heightDp = 150)
@Composable
fun PreviewIdle() {
    Box(Modifier.padding(20.dp)) {
        IslandState.mode.value = IslandMode.IDLE
        IslandState.isManuallyCollapsed.value = false
        DynamicIslandUI()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A, widthDp = 400, heightDp = 200)
@Composable
fun PreviewMusicExpanded() {
    Box(Modifier.padding(20.dp)) {
        IslandState.mode.value = IslandMode.MUSIC
        IslandState.isManuallyCollapsed.value = false
        IslandState.musicInfo.value = MusicInfo(
            title = "Perfect", artist = "Ed Sheeran", isPlaying = true
        )
        DynamicIslandUI()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A, widthDp = 400, heightDp = 150)
@Composable
fun PreviewMusicCollapsed() {
    Box(Modifier.padding(20.dp)) {
        IslandState.mode.value = IslandMode.MUSIC
        IslandState.isManuallyCollapsed.value = true
        IslandState.musicInfo.value = MusicInfo(
            title = "Perfect", artist = "Ed Sheeran", isPlaying = true
        )
        DynamicIslandUI()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A, widthDp = 400, heightDp = 200)
@Composable
fun PreviewChat() {
    Box(Modifier.padding(20.dp)) {
        IslandState.mode.value = IslandMode.CHAT
        IslandState.chatInfo.value = ChatInfo(
            senderName = "Ibu",
            message = "Sudah makan belum nak?",
            avatarInitial = "I",
            packageName = "com.whatsapp"
        )
        DynamicIslandUI()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A, widthDp = 400, heightDp = 200)
@Composable
fun PreviewCallRinging() {
    Box(Modifier.padding(20.dp)) {
        IslandState.mode.value = IslandMode.CALL_RINGING
        IslandState.callInfo.value = CallInfo("Budi Santoso", "+62 812...", "B", "com.whatsapp")
        DynamicIslandUI()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A, widthDp = 400, heightDp = 150)
@Composable
fun PreviewCallActive() {
    Box(Modifier.padding(20.dp)) {
        IslandState.mode.value = IslandMode.CALL_ACTIVE
        IslandState.callInfo.value = CallInfo("Budi Santoso", "", "B", "com.whatsapp")
        DynamicIslandUI()
    }
}
