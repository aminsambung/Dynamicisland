package com.example.dynamicisland

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// === Warna ===
private val IslandBlack = Color(0xFF0A0A0A)
private val GreenAccept = Color(0xFF30D158)
private val RedDecline = Color(0xFFFF3B30)
private val SpotifyGreen = Color(0xFF1DB954)

@Composable
fun DynamicIslandUI() {
    val mode = IslandState.mode.value
    val call = IslandState.callInfo.value

    val targetWidth = when (mode) {
        IslandMode.IDLE -> 130.dp
        IslandMode.MUSIC -> 340.dp
        IslandMode.CALL_RINGING -> 360.dp
        IslandMode.CALL_ACTIVE -> 240.dp
    }
    val targetHeight = when (mode) {
        IslandMode.IDLE -> 36.dp
        IslandMode.MUSIC -> 72.dp
        IslandMode.CALL_RINGING -> 100.dp
        IslandMode.CALL_ACTIVE -> 40.dp
    }

    val width by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 350f),
        label = "w"
    )
    val height by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 350f),
        label = "h"
    )

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(percent = 50))
            .background(IslandBlack),
        contentAlignment = Alignment.Center
    ) {
        when (mode) {
            IslandMode.IDLE -> IdleContent()
            IslandMode.MUSIC -> MusicContent()
            IslandMode.CALL_RINGING -> CallRingingContent(call)
            IslandMode.CALL_ACTIVE -> CallActiveContent(call)
        }
    }
}

/* ---------------- IDLE ---------------- */
@Composable
private fun IdleContent() {
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF00E676))
        )
        Text("• • •", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
    }
}

/* ---------------- MUSIC ---------------- */
@Composable
private fun MusicContent() {
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(42.dp).clip(CircleShape).background(SpotifyGreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MusicNote, null, tint = Color.White)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                "Now Playing",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text("Lagu favoritmu 🎵", color = Color.Gray, fontSize = 11.sp)
        }
    }
}

/* ---------------- INCOMING CALL ---------------- */
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
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar + pulse ring
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(56.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(GreenAccept.copy(alpha = 0.25f))
            )
            Box(
                Modifier.size(46.dp).clip(CircleShape).background(Color(0xFF2C2C2E)),
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
                maxLines = 1
            )
            Text(
                "Panggilan masuk…",
                color = Color.Gray,
                fontSize = 11.sp,
                maxLines = 1
            )
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

/* ---------------- ACTIVE CALL ---------------- */
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
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(26.dp).clip(CircleShape).background(GreenAccept),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Call,
                null,
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
                maxLines = 1
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

/* ---------------- Reusable ---------------- */
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
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(iconSize))
    }
}

/* ---------------- Preview ---------------- */
@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A, widthDp = 400, heightDp = 150)
@Composable
fun PreviewIdle() {
    Box(Modifier.padding(20.dp)) {
        IslandState.mode.value = IslandMode.IDLE
        DynamicIslandUI()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A, widthDp = 400, heightDp = 150)
@Composable
fun PreviewMusic() {
    Box(Modifier.padding(20.dp)) {
        IslandState.mode.value = IslandMode.MUSIC
        DynamicIslandUI()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A, widthDp = 400, heightDp = 200)
@Composable
fun PreviewCallRinging() {
    Box(Modifier.padding(20.dp)) {
        IslandState.mode.value = IslandMode.CALL_RINGING
        IslandState.callInfo.value = CallInfo("Budi Santoso", "+62 812...", "B")
        DynamicIslandUI()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A, widthDp = 400, heightDp = 150)
@Composable
fun PreviewCallActive() {
    Box(Modifier.padding(20.dp)) {
        IslandState.mode.value = IslandMode.CALL_ACTIVE
        IslandState.callInfo.value = CallInfo("Budi Santoso")
        DynamicIslandUI()
    }
}
