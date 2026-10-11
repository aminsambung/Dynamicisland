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
// WARNA AKSEN (fixed)
// ============================================
private val GreenAccept = Color(0xFF30D158)
private val RedDecline = Color(0xFFFF3B30)
private val SpotifyGreen = Color(0xFF1DB954)
private val WhatsAppGreen = Color(0xFF25D366)
private val CameraDotGreen = Color(0xFF00E676)
private val AvatarBg = Color(0xFF2C2C2E)
private val ChargingGreen = Color(0xFF34C759)
private val OrangeAccent = Color(0xFFFF9500)
private val YellowAccent = Color(0xFFFFCC00)
private val NavGreen = Color(0xFF1B5E20)
private val NavTurnGreen = Color(0xFF34C759)
private val NavTurnGray = Color(0xFF8E8E93)
private val NavTurnRed = Color(0xFFFF3B30)

// ============================================
// MAIN UI
// ============================================
@Composable
fun DynamicIslandUI() {
    val context = LocalContext.current
    val mode = IslandState.mode.value
    val call = IslandState.callInfo.value
    val isCollapsed = IslandState.shouldShowCollapsed()

    // ============ BACA PREFERENSI USER ============
    val rawAlpha = IslandState.glassAlpha.value
    val borderAlpha = IslandState.borderAlpha.value
    val colorTop = Color(IslandState.islandColor.value)
    val colorBottom = Color(IslandState.islandColorBottom.value)
    val useGradient = IslandState.useGradient.value

    // ============================================
    // AUTO-ADJUST: Warna gelap minimal 60%
    // ============================================
    val glassAlpha = if (isDarkColor(IslandState.islandColor.value)) {
        maxOf(rawAlpha, 0.6f)   // Warna gelap → min 60%
    } else {
        rawAlpha                 // Warna terang → bebas
    }

    val targetWidth = when {
        isCollapsed -> when (mode) {
            IslandMode.CHARGING -> 180.dp
            IslandMode.NAVIGATION -> 150.dp
            IslandMode.ALARM -> 150.dp
            else -> 130.dp
        }
        mode == IslandMode.IDLE -> 130.dp
        mode == IslandMode.MUSIC -> 340.dp
        mode == IslandMode.CALL_RINGING -> 360.dp
        mode == IslandMode.CALL_ACTIVE -> 240.dp
        mode == IslandMode.CHAT -> 340.dp
        mode == IslandMode.CHARGING -> 340.dp
        mode == IslandMode.NAVIGATION -> 370.dp
        mode == IslandMode.ALARM -> 340.dp
        else -> 130.dp
    }
    val targetHeight = when {
        isCollapsed -> 36.dp
        mode == IslandMode.IDLE -> 36.dp
        mode == IslandMode.MUSIC -> 72.dp
        mode == IslandMode.CALL_RINGING -> 100.dp
        mode == IslandMode.CALL_ACTIVE -> 40.dp
        mode == IslandMode.CHAT -> 72.dp
        mode == IslandMode.CHARGING -> 72.dp
        mode == IslandMode.NAVIGATION -> 110.dp
        mode == IslandMode.ALARM -> 72.dp
        else -> 36.dp
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

    // ============================================
    // HITUNG WARNA GRADIENT (solid / gradient)
    // ============================================
    val gradientColors = if (useGradient) {
        listOf(
            colorTop.copy(alpha = glassAlpha),
            colorBottom.copy(alpha = glassAlpha * 0.85f)
        )
    } else {
        listOf(
            colorTop.copy(alpha = glassAlpha),
            colorTop.copy(alpha = glassAlpha * 0.85f)
        )
    }

    // WRAPPER FULL WIDTH — biar pill selalu center
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .width(width)
                .height(height)
                .clip(RoundedCornerShape(percent = 50))
                .background(
                    brush = Brush.verticalGradient(colors = gradientColors)
                )
                .border(
                    width = 0.5.dp,
                    color = Color.White.copy(
                        alpha = if (isDarkColor(IslandState.islandColor.value)) {
                            maxOf(borderAlpha, 0.25f)   // Auto-border untuk warna gelap
                        } else {
                            borderAlpha
                        }
                    ),
                    shape = RoundedCornerShape(percent = 50)
                )
                // ============ TAP ============
                .pointerInput(mode, isCollapsed) {
                    detectTapGestures(
                        onTap = {
                            if (isCollapsed) {
                                IslandState.toggleCollapse()
                            } else {
                                when (mode) {
                                    IslandMode.MUSIC -> MediaControlBridge.playPause()
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
                                    IslandMode.ALARM -> {
                                        openApp(context, IslandState.alarmInfo.value.packageName)
                                        IslandState.dismissAlarm()
                                    }
                                    IslandMode.CHARGING -> {
                                        try {
                                            val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
                                                .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    }
                                    IslandMode.NAVIGATION -> {
                                        openApp(context, IslandState.navigationInfo.value.packageName)
                                    }
                                    IslandMode.IDLE -> {
                                        if (!IslandState.musicInfo.value.isEmpty) {
                                            IslandState.showMusic()
                                        }
                                    }
                                    else -> {}
                                }
                            }
                        },
                        onDoubleTap = {
                            when (mode) {
                                IslandMode.MUSIC,
                                IslandMode.CHARGING,
                                IslandMode.NAVIGATION,
                                IslandMode.ALARM -> IslandState.toggleCollapse()
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
                                IslandMode.ALARM -> {
                                    if (abs(totalDrag) > 80f) IslandState.dismissAlarm()
                                }
                                else -> {}
                            }
                            totalDrag = 0f
                        },
                        onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (isCollapsed) {
                when (mode) {
                    IslandMode.MUSIC -> MiniMusicContent()
                    IslandMode.CHARGING -> MiniChargingContent()
                    IslandMode.NAVIGATION -> MiniNavigationContent()
                    IslandMode.ALARM -> MiniAlarmContent()
                    else -> IdleContent()
                }
            } else {
                when (mode) {
                    IslandMode.IDLE -> IdleContent()
                    IslandMode.MUSIC -> MusicContent()
                    IslandMode.CALL_RINGING -> CallRingingContent(call)
                    IslandMode.CALL_ACTIVE -> CallActiveContent(call)
                    IslandMode.CHAT -> ChatContent()
                    IslandMode.CHARGING -> ChargingContent()
                    IslandMode.NAVIGATION -> NavigationContent()
                    IslandMode.ALARM -> AlarmContent()
                }
            }
        }
    }
}

// ============================================
// HELPER — Deteksi warna gelap
// ============================================
private fun isDarkColor(colorLong: Long): Boolean {
    val r = ((colorLong shr 16) and 0xFF) / 255f
    val g = ((colorLong shr 8) and 0xFF) / 255f
    val b = (colorLong and 0xFF) / 255f

    // Formula luminance: 0.299*R + 0.587*G + 0.114*B
    val luminance = 0.299f * r + 0.587f * g + 0.114f * b

    // < 0.3 = gelap
    return luminance < 0.3f
}

// ============================================
// IDLE
// ============================================
@Composable
private fun IdleContent() {
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(CameraDotGreen))
        Text("• • •", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
    }
}

// ============================================
// MINI MUSIC
// ============================================
@Composable
private fun MiniMusicContent() {
    val music = IslandState.musicInfo.value
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        RotatingAlbumArt(20.dp, music.isPlaying, music.albumArt)
        if (music.isPlaying) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val heights = listOf(5.dp, 9.dp, 5.dp)
                repeat(3) { i ->
                    Box(
                        Modifier.padding(horizontal = 1.dp).width(2.dp).height(heights[i])
                            .clip(RoundedCornerShape(1.dp)).background(SpotifyGreen)
                    )
                }
            }
        }
    }
}

// ============================================
// MINI CHARGING ⚡
// ============================================
@Composable
private fun MiniChargingContent() {
    val info = IslandState.chargingInfo.value
    val isFull = info.isFull || info.percentage >= 100
    val accentColor = if (isFull) Color(0xFF30D158) else ChargingGreen

    val infinite = rememberInfiniteTransition(label = "pulse_mini")
    val pulse by infinite.animateFloat(
        0.85f, 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "⚡",
            fontSize = 13.sp,
            color = accentColor,
            modifier = Modifier.graphicsLayer {
                if (!isFull) { scaleX = pulse; scaleY = pulse }
            }
        )
        Spacer(Modifier.width(6.dp))
        Text(
            "${info.percentage}%",
            color = accentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Spacer(Modifier.width(8.dp))
        BatteryBar(
            percentage = info.percentage,
            color = accentColor,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(6.dp))
        Text("🔋", fontSize = 11.sp)
    }
}

// ============================================
// BATTERY BAR
// ============================================
@Composable
private fun BatteryBar(
    percentage: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val clamped = percentage.coerceIn(0, 100)

    val animatedFill by animateFloatAsState(
        targetValue = clamped / 100f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "battery_fill"
    )

    Box(
        modifier = modifier
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = 0.15f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedFill)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(color.copy(alpha = 0.75f), color)
                    )
                )
        )
        if (clamped >= 100) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.35f),
                                Color.Transparent,
                                Color.White.copy(alpha = 0.35f)
                            )
                        )
                    )
            )
        }
    }
}

// ============================================
// MINI NAVIGATION 🗺️
// ============================================
@Composable
private fun MiniNavigationContent() {
    val nav = IslandState.navigationInfo.value
    val turnIcon = getTurnIcon(nav.instruction)

    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(16.dp).clip(CircleShape).background(NavTurnGreen),
                contentAlignment = Alignment.Center
            ) {
                Text(turnIcon, color = Color.White, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(6.dp))
            Text(
                nav.distance.ifEmpty { nav.duration }.ifEmpty { nav.appName },
                color = Color.White, fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text("⬆", color = Color.White, fontSize = 12.sp)
    }
}

// ============================================
// MINI ALARM ⏰
// ============================================
@Composable
private fun MiniAlarmContent() {
    val alarm = IslandState.alarmInfo.value
    val isRinging = alarm.isRinging || alarm.minutesUntil <= 0
    val accentColor = when {
        isRinging -> RedDecline
        alarm.minutesUntil <= 1 -> RedDecline
        alarm.minutesUntil <= 5 -> YellowAccent
        else -> OrangeAccent
    }

    val infinite = rememberInfiniteTransition(label = "bell_mini")
    val pulse by infinite.animateFloat(
        initialValue = 0.9f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isRinging) 500 else 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(16.dp).clip(CircleShape).background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Text("⏰", fontSize = 9.sp,
                    modifier = Modifier.graphicsLayer { scaleX = pulse; scaleY = pulse })
            }
            Spacer(Modifier.width(6.dp))
            Text(
                alarm.time.ifEmpty { "—" },
                color = Color.White, fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (alarm.minutesUntil > 0) {
                Spacer(Modifier.width(4.dp))
                Text("${alarm.minutesUntil}m", color = accentColor,
                    fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            } else if (isRinging) {
                Spacer(Modifier.width(4.dp))
                Text("NOW!", color = accentColor, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold)
            }
        }
        Text("🔔", fontSize = 12.sp)
    }
}

// ============================================
// MUSIC (expanded)
// ============================================
@Composable
private fun MusicContent() {
    val music = IslandState.musicInfo.value
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RotatingAlbumArt(46.dp, music.isPlaying, music.albumArt)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                music.title.ifEmpty { "Tidak ada lagu" },
                color = Color.White, fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                music.artist.ifEmpty { "—" },
                color = Color.Gray, fontSize = 11.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        if (music.isPlaying) EqualizerBars()
    }
}

// ============================================
// ROTATING ALBUM ART
// ============================================
@Composable
private fun RotatingAlbumArt(size: Dp, isPlaying: Boolean, albumArt: Bitmap?) {
    val infinite = rememberInfiniteTransition(label = "rotate")
    val rotation by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "rotation"
    )
    val currentRotation = if (isPlaying) rotation else 0f

    Box(
        modifier = Modifier.size(size).graphicsLayer { rotationZ = currentRotation }
            .clip(CircleShape).background(SpotifyGreen),
        contentAlignment = Alignment.Center
    ) {
        if (albumArt != null && !albumArt.isRecycled) {
            Image(albumArt.asImageBitmap(), null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape))
        } else {
            Text("🎵", fontSize = (size.value * 0.5f).sp, color = Color.White)
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
        0f, 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "phase"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            val h = (6 + ((phase * 10 + i * 3) % 10)).dp
            Box(Modifier.padding(horizontal = 1.dp).width(3.dp).height(h)
                .clip(RoundedCornerShape(2.dp)).background(SpotifyGreen))
        }
    }
}

// ============================================
// CHAT 💬
// ============================================
@Composable
private fun ChatContent() {
    val chat = IslandState.chatInfo.value
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(WhatsAppGreen),
            contentAlignment = Alignment.Center) {
            Text(chat.avatarInitial.ifEmpty { "?" }, color = Color.White,
                fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(chat.senderName.ifEmpty { "Pesan Baru" }, color = Color.White,
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text(chat.message, color = Color.Gray, fontSize = 11.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("💬", fontSize = 16.sp)
            Spacer(Modifier.width(4.dp))
            Text("›", color = Color.White.copy(alpha = 0.5f), fontSize = 18.sp,
                fontWeight = FontWeight.Bold)
        }
    }
}

// ============================================
// CALL RINGING 📞
// ============================================
@Composable
private fun CallRingingContent(call: CallInfo) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        1f, 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.size(56.dp).scale(scale).clip(CircleShape)
                .background(GreenAccept.copy(alpha = 0.25f)))
            Box(Modifier.size(46.dp).clip(CircleShape).background(AvatarBg),
                contentAlignment = Alignment.Center) {
                Text(call.avatarInitial, color = Color.White, fontSize = 20.sp,
                    fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(call.name, color = Color.White, fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Panggilan masuk", color = Color.Gray, fontSize = 11.sp, maxLines = 1)
                Spacer(Modifier.width(4.dp))
                Text("›", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp,
                    fontWeight = FontWeight.Bold)
            }
        }
        CallButtonEmoji("📵", RedDecline) { IslandState.endCall() }
        Spacer(Modifier.width(8.dp))
        CallButtonEmoji("📞", GreenAccept) { IslandState.acceptCall() }
    }
}

// ============================================
// CALL ACTIVE
// ============================================
@Composable
private fun CallActiveContent(call: CallInfo) {
    var seconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) { delay(1000); seconds++ }
    }
    val timeText = "%02d:%02d".format(seconds / 60, seconds % 60)
    val infinite = rememberInfiniteTransition(label = "wave")
    val wave by infinite.animateFloat(
        0.4f, 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "wave"
    )
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(26.dp).clip(CircleShape).background(GreenAccept),
            contentAlignment = Alignment.Center) {
            Text("📞", fontSize = 12.sp)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(call.name, color = Color.White, fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text(timeText, color = GreenAccept, fontSize = 10.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(4) { i ->
                val h = (6 + (i * 3)) * wave
                Box(Modifier.padding(horizontal = 1.dp).width(3.dp).height(h.dp)
                    .clip(RoundedCornerShape(2.dp)).background(GreenAccept))
            }
        }
        Spacer(Modifier.width(10.dp))
        CallButtonEmoji("📵", RedDecline, 32.dp) { IslandState.endCall() }
    }
}

// ============================================
// CHARGING ⚡ (expanded)
// ============================================
@Composable
private fun ChargingContent() {
    val info = IslandState.chargingInfo.value
    val isFull = info.isFull || info.percentage >= 100
    val accentColor = if (isFull) Color(0xFF30D158) else ChargingGreen

    val infinite = rememberInfiniteTransition(label = "pulse")
    val pulse by infinite.animateFloat(
        0.85f, 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center) {
            Text("⚡", fontSize = 16.sp, color = accentColor,
                modifier = Modifier.graphicsLayer {
                    if (!isFull) { scaleX = pulse; scaleY = pulse }
                })
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem(info.timeToFull.ifEmpty { "—" }, Color.White)
                StatItem(if (info.voltage > 0) "%.1f V".format(info.voltage) else "—", Color.White)
                StatItem(if (info.temperature > 0) "%.1f°C".format(info.temperature) else "—", Color.White)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem("${info.percentage}%", accentColor, bold = true)
                StatItem(info.chargeType.ifEmpty { "—" }, Color.White.copy(alpha = 0.75f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(accentColor))
                    Spacer(Modifier.width(4.dp))
                    StatItem(if (isFull) "Full" else "OK", Color.White.copy(alpha = 0.75f))
                }
            }
        }
        Spacer(Modifier.width(6.dp))
        Box(Modifier.size(22.dp).clip(CircleShape).background(OrangeAccent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center) {
            Text("›", color = OrangeAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatItem(value: String, color: Color, bold: Boolean = false) {
    Text(value, color = color, fontSize = 11.sp,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
}

// ============================================
// NAVIGATION 🗺️
// ============================================
@Composable
private fun NavigationContent() {
    val nav = IslandState.navigationInfo.value

    val turnIcon = getTurnIcon(nav.instruction)
    val turnColor = getTurnColor(nav.instruction)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavGreen)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(turnColor),
                contentAlignment = Alignment.Center
            ) {
                Text(turnIcon, color = Color.White, fontSize = 22.sp,
                    fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    nav.instruction.ifEmpty { "Lanjut lurus" },
                    color = Color.White, fontSize = 13.sp,
                    fontWeight = FontWeight.Bold, maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    buildString {
                        if (nav.duration.isNotEmpty()) append(nav.duration)
                        if (nav.distanceTotal.isNotEmpty()) {
                            if (isNotEmpty()) append(" · ")
                            append(nav.distanceTotal)
                            append(" remaining")
                        }
                    }.ifEmpty { nav.appName },
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable {
                        IslandState.dismissNavigation()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("✕", color = Color.White, fontSize = 12.sp,
                    fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                nav.distance.ifEmpty { "—" },
                color = Color.White, fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable {
                        IslandState.dismissNavigation()
                    }
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    "Exit Navigation",
                    color = Color.White, fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ============================================
// HELPER — Icon belok dari instruksi
// ============================================
private fun getTurnIcon(instruction: String): String {
    val lower = instruction.lowercase()
    return when {
        lower.contains("uturn") || lower.contains("u-turn") -> "↩"
        lower.contains("roundabout") -> "🔄"
        lower.contains("left") || lower.contains("kiri") -> "⬅"
        lower.contains("right") || lower.contains("kanan") -> "➡"
        lower.contains("straight") || lower.contains("lurus") -> "⬆"
        lower.contains("exit") || lower.contains("keluar") -> "↗"
        lower.contains("merge") -> "↗"
        lower.contains("slight left") -> "↖"
        lower.contains("slight right") -> "↗"
        lower.contains("sharp left") -> "⬅"
        lower.contains("sharp right") -> "➡"
        else -> "⬆"
    }
}

private fun getTurnColor(instruction: String): Color {
    val lower = instruction.lowercase()
    return when {
        lower.contains("uturn") || lower.contains("u-turn") -> NavTurnRed
        lower.contains("left") || lower.contains("kiri") -> NavTurnGreen
        lower.contains("right") || lower.contains("kanan") -> NavTurnGreen
        lower.contains("straight") || lower.contains("lurus") -> NavTurnGray
        else -> NavTurnGreen
    }
}

// ============================================
// ALARM ⏰
// ============================================
@Composable
private fun AlarmContent() {
    val alarm = IslandState.alarmInfo.value
    val isRinging = alarm.isRinging || alarm.minutesUntil <= 0

    val accentColor = when {
        isRinging -> RedDecline
        alarm.minutesUntil <= 1 -> RedDecline
        alarm.minutesUntil <= 5 -> YellowAccent
        else -> OrangeAccent
    }

    val infinite = rememberInfiniteTransition(label = "bell")
    val pulse by infinite.animateFloat(
        initialValue = if (isRinging) 0.9f else 0.95f,
        targetValue = if (isRinging) 1.25f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isRinging) 500 else 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(accentColor),
            contentAlignment = Alignment.Center) {
            Text("⏰", fontSize = 22.sp,
                modifier = Modifier.graphicsLayer { scaleX = pulse; scaleY = pulse })
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(alarm.time.ifEmpty { "—" }, color = Color.White,
                    fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when {
                        isRinging -> "NOW!"
                        alarm.minutesUntil > 0 -> "${alarm.minutesUntil} min"
                        else -> ""
                    },
                    color = accentColor, fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold, maxLines = 1
                )
            }
            Text(alarm.label.ifEmpty { "Alarm" }, color = Color.Gray,
                fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🔔", fontSize = 18.sp,
                modifier = Modifier.graphicsLayer {
                    if (isRinging) { scaleX = pulse; scaleY = pulse }
                })
            Spacer(Modifier.width(4.dp))
            Text("›", color = Color.White.copy(alpha = 0.5f), fontSize = 18.sp,
                fontWeight = FontWeight.Bold)
        }
    }
}

// ============================================
// REUSABLE — Tombol Call (emoji)
// ============================================
@Composable
private fun CallButtonEmoji(
    emoji: String, bg: Color, size: Dp = 40.dp,
    onClick: () -> Unit
) {
    Box(
        Modifier.size(size).clip(CircleShape).background(bg).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = (size.value * 0.45f).sp)
    }
}

// ============================================
// HELPER — Buka app
// ============================================
private fun openApp(context: Context, packageName: String) {
    if (packageName.isEmpty()) return
    context.packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        try { context.startActivity(intent) } catch (_: Exception) {}
    }
}

private fun openMusicApp(context: Context) {
    val packageName = IslandState.musicInfo.value.packageName
    if (packageName.isNotEmpty()) {
        context.packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            try { context.startActivity(intent); return } catch (_: Exception) {}
        }
    }
    val fallback = listOf(
        "com.spotify.music", "com.google.android.apps.youtube.music",
        "in.krosbits.musicolet", "com.maxmpz.audioplayer",
        "org.videolan.vlc", "com.miui.player", "com.samsung.android.music"
    )
    for (pkg in fallback) {
        context.packageManager.getLaunchIntentForPackage(pkg)?.let { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            try { context.startActivity(intent); return } catch (_: Exception) {}
        }
    }
}

private fun openDialer(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_DIAL).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent)
    } catch (_: Exception) {}
}

// ============================================
// PREVIEWS
// ============================================
@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A, widthDp = 400, heightDp = 200)
@Composable
fun PreviewIdle() {
    Box(Modifier.padding(20.dp)) {
        IslandState.mode.value = IslandMode.IDLE
        DynamicIslandUI()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A, widthDp = 400, heightDp = 200)
@Composable
fun PreviewMusicGradient() {
    Box(Modifier.padding(20.dp)) {
        IslandState.mode.value = IslandMode.MUSIC
        IslandState.musicInfo.value = MusicInfo(
            title = "Perfect", artist = "Ed Sheeran", isPlaying = true
        )
        IslandState.useGradient.value = true
        IslandState.islandColor.value = 0xFFE65100
        IslandState.islandColorBottom.value = 0xFFC62828
        DynamicIslandUI()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A, widthDp = 400, heightDp = 200)
@Composable
fun PreviewCharging() {
    Box(Modifier.padding(20.dp)) {
        IslandState.mode.value = IslandMode.CHARGING
        IslandState.chargingInfo.value = ChargingInfo(
            percentage = 34, voltage = 3.8f, temperature = 40.3f,
            timeToFull = "05:59", chargeType = "USB", isCharging = true
        )
        DynamicIslandUI()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A, widthDp = 400, heightDp = 200)
@Composable
fun PreviewNavigation() {
    Box(Modifier.padding(20.dp)) {
        IslandState.mode.value = IslandMode.NAVIGATION
        IslandState.navigationInfo.value = NavigationInfo(
            instruction = "Turn Right on MG Road",
            distance = "90m", duration = "12 min",
            distanceTotal = "5.2 km", eta = "22:33",
            appName = "Maps", packageName = "com.google.android.apps.maps"
        )
        DynamicIslandUI()
    }
}
