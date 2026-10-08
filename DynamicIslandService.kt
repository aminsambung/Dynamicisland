package com.example.dynamicisland

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

class DynamicIslandService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var composeView: ComposeView
    private lateinit var lifecycleOwner: OverlayLifecycleOwner

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        createOverlay()
    }

    private fun startForegroundNotification() {
        val channelId = "island_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Dynamic Island",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                setShowBadge(false)
            }
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }

        val notification: Notification =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(this, channelId)
                    .setContentTitle("Dynamic Island aktif")
                    .setSmallIcon(android.R.drawable.ic_menu_info_details)
                    .setOngoing(true)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(this)
                    .setContentTitle("Dynamic Island aktif")
                    .setSmallIcon(android.R.drawable.ic_menu_info_details)
                    .setOngoing(true)
                    .build()
            }

        startForeground(1, notification)
    }

    private fun createOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        lifecycleOwner = OverlayLifecycleOwner().apply {
            onCreate()
            onStart()
            onResume()
        }

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                DynamicIslandUI()
            }
        }

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 40
        }

        windowManager.addView(composeView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::composeView.isInitialized) {
            try {
                windowManager.removeView(composeView)
            } catch (_: Exception) {}
        }
        if (::lifecycleOwner.isInitialized) {
            lifecycleOwner.onDestroy()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}