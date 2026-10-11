package com.example.dynamicisland

import android.content.Context
import android.content.SharedPreferences

/**
 * Simpan preferensi notifikasi per app.
 * User pilih app mana yang boleh muncul di island.
 */
object NotificationPreferences {

    private const val PREF_NAME = "notification_prefs"
    private const val KEY_PREFIX = "app_"

    // Default enabled
    private val DEFAULT_ENABLED = setOf(
        "com.whatsapp",
        "com.whatsapp.w4b",
        "org.telegram.messenger",
        "com.instagram.android",
        "com.facebook.orca",
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.android.mms",
        "com.google.android.gm",
        "com.microsoft.office.outlook"
    )

    // Daftar app yang bisa dipilih user
    val AVAILABLE_APPS = listOf(
        AppItem("WhatsApp", "com.whatsapp", "💬"),
        AppItem("WhatsApp Business", "com.whatsapp.w4b", "💼"),
        AppItem("Telegram", "org.telegram.messenger", "✈️"),
        AppItem("Instagram", "com.instagram.android", "📸"),
        AppItem("Facebook", "com.facebook.katana", "📘"),
        AppItem("Messenger", "com.facebook.orca", "💬"),
        AppItem("Twitter / X", "com.twitter.android", "🐦"),
        AppItem("TikTok", "com.tiktok", "🎵"),
        AppItem("Snapchat", "com.snapchat.android", "👻"),
        AppItem("Discord", "com.discord", "🎮"),
        AppItem("Slack", "com.slack", "💼"),
        AppItem("Microsoft Teams", "com.microsoft.teams", "💼"),
        AppItem("LINE", "com.linecorp.line", "💚"),
        AppItem("WeChat", "com.tencent.mm", "💚"),
        AppItem("Viber", "com.viber.voip", "💜"),
        AppItem("Skype", "com.skype.raider", "💙"),
        AppItem("Google Messages", "com.google.android.apps.messaging", "💬"),
        AppItem("Samsung Messages", "com.samsung.android.messaging", "💬"),
        AppItem("Gmail", "com.google.android.gm", "📧"),
        AppItem("Outlook", "com.microsoft.office.outlook", "📧"),
        AppItem("Yahoo Mail", "com.yahoo.mobile.client.android.mail", "📧"),
        AppItem("Shopee", "com.shopee.id", "🛒"),
        AppItem("Tokopedia", "com.tokopedia.tkpd", "🛒"),
        AppItem("Lazada", "com.lazada.android", "🛒"),
        AppItem("Gojek", "com.gojek.app", "🛵"),
        AppItem("Grab", "com.grabtaxi.passenger", "🚗"),
        AppItem("GoPay", "com.gojek.gopay", "💰"),
        AppItem("DANA", "com.dana", "💰"),
        AppItem("OVO", "id.co.ovo", "💰"),
        AppItem("BNI Wondr", "com.bni.wondr", "🏦"),
        AppItem("BCA Mobile", "com.bca.mybca.omni.android", "🏦"),
        AppItem("Mandiri Online", "id.co.bankmandiri.mandirionline", "🏦"),
        AppItem("BRI BRImo", "com.bri.brimo", "🏦"),
        AppItem("YouTube", "com.google.android.youtube", "📺"),
        AppItem("Netflix", "com.netflix.mediaclient", "🎬"),
        AppItem("Spotify", "com.spotify.music", "🎵"),
        AppItem("LinkedIn", "com.linkedin.android", "💼"),
        AppItem("Reddit", "com.reddit.frontpage", "🤖"),
        AppItem("Pinterest", "com.pinterest", "📌"),
        AppItem("Google Photos", "com.google.android.apps.photos", "📷"),
    )

    // Data class
    data class AppItem(
        val name: String,
        val packageName: String,
        val emoji: String
    )

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Cek apakah app ini enabled (user izinkan muncul di island).
     */
    fun isAppEnabled(context: Context, packageName: String): Boolean {
        val defaultValue = packageName in DEFAULT_ENABLED
        return prefs(context).getBoolean(KEY_PREFIX + packageName, defaultValue)
    }

    /**
     * Set enabled/disabled untuk app.
     */
    fun setAppEnabled(context: Context, packageName: String, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PREFIX + packageName, enabled).apply()
    }

    /**
     * Cek apakah package ada di daftar yang bisa di-setting.
     */
    fun isAppAvailable(packageName: String): Boolean {
        return AVAILABLE_APPS.any { it.packageName == packageName }
    }

    /**
     * Reset semua setting ke default.
     */
    fun resetAll(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
