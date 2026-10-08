package com.example.dynamicisland

import android.app.AlarmManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import java.text.SimpleDateFormat
import java.util.*

/**
 * Cek alarm berikutnya dari AlarmManager setiap 30 detik.
 * Kalau alarm H-10 menit → tampilkan di island.
 */
object AlarmScheduler {

    private var handler: Handler? = null
    private var runnable: Runnable? = null
    private var lastShownAlarmTime: Long = 0L

    fun start(context: Context) {
        stop()
        handler = Handler(Looper.getMainLooper())
        runnable = object : Runnable {
            override fun run() {
                checkNextAlarm(context)
                handler?.postDelayed(this, 30_000L)  // cek setiap 30 detik
            }
        }
        handler?.post(runnable!!)
    }

    fun stop() {
        runnable?.let { handler?.removeCallbacks(it) }
        handler = null
        runnable = null
    }

    private fun checkNextAlarm(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                ?: return
            val nextAlarm = alarmManager.nextAlarmClock ?: return

            val triggerTime = nextAlarm.triggerTime
            val now = System.currentTimeMillis()
            val diffMs = triggerTime - now

            // Kalau alarm dalam 10 menit ke depan
            if (diffMs in 0..(10 * 60 * 1000L)) {
                val minutesUntil = (diffMs / 60_000L).toInt()

                // Format waktu alarm
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                val timeStr = sdf.format(Date(triggerTime))

                // Cek kalau ini alarm yang sama dengan sebelumnya
                if (lastShownAlarmTime == triggerTime) {
                    // Sudah ditampilkan → cuma update countdown
                    IslandState.updateAlarmCountdown(minutesUntil, timeStr)
                } else {
                    // Alarm baru → show dengan label default
                    lastShownAlarmTime = triggerTime
                    IslandState.showAlarm(
                        AlarmInfo(
                            time = timeStr,
                            label = "Alarm",
                            minutesUntil = minutesUntil,
                            isRinging = false,
                            packageName = ""
                        )
                    )
                }
            } else {
                // Alarm sudah lewat / lebih dari 10 menit
                if (lastShownAlarmTime != 0L && lastShownAlarmTime < now) {
                    // Alarm sudah lewat
                    if (IslandState.mode.value == IslandMode.ALARM &&
                        !IslandState.alarmInfo.value.isRinging) {
                        IslandState.clearAlarm()
                    }
                    lastShownAlarmTime = 0L
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AlarmScheduler", "Error: ${e.message}")
        }
    }
}
