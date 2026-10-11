package com.example.dynamicisland

import android.content.Context
import android.content.SharedPreferences

/**
 * Simpan preferensi user (transparansi, warna, gradient).
 */
object IslandPreferences {

    private const val PREF_NAME = "island_prefs"
    private const val KEY_GLASS_ALPHA = "glass_alpha"
    private const val KEY_BORDER_ALPHA = "border_alpha"
    private const val KEY_ISLAND_COLOR = "island_color"
    private const val KEY_ISLAND_COLOR_BOTTOM = "island_color_bottom"
    private const val KEY_USE_GRADIENT = "use_gradient"

    const val DEFAULT_GLASS_ALPHA = 0.67f
    const val DEFAULT_BORDER_ALPHA = 0.15f
    const val DEFAULT_COLOR = 0xFF0A0A0A

    // ============================================
    // PRESET WARNA
    // ============================================
    val COLOR_PRESETS = listOf(
        ColorPreset("Hitam",     0xFF0A0A0A),
        ColorPreset("Gray",      0xFF2C2C2E),
        ColorPreset("Biru",      0xFF1E88E5),
        ColorPreset("Biru Tua",  0xFF0D47A1),
        ColorPreset("Ungu",      0xFF6A1B9A),
        ColorPreset("Hijau",     0xFF2E7D32),
        ColorPreset("Orange",    0xFFE65100),
        ColorPreset("Merah",     0xFFC62828),
        ColorPreset("Pink",      0xFFAD1457),
        ColorPreset("Teal",      0xFF00695C),
        ColorPreset("Kuning",    0xFFF9A825),
        ColorPreset("Cyan",      0xFF00838F),
        ColorPreset("Putih",     0xFFFFFFFF),
        ColorPreset("Navy",      0xFF001F3F),
        ColorPreset("Maroon",    0xFF800000),
        ColorPreset("Olive",     0xFF808000),
    )

    data class ColorPreset(val name: String, val color: Long)

    // ============================================
    // PRESET GRADIENT
    // ============================================
    val GRADIENT_PRESETS = listOf(
        GradientPreset("Sunset",    0xFFE65100, 0xFFC62828),
        GradientPreset("Ocean",     0xFF1E88E5, 0xFF0D47A1),
        GradientPreset("Purple",    0xFF6A1B9A, 0xFF0D47A1),
        GradientPreset("Forest",    0xFF2E7D32, 0xFF00695C),
        GradientPreset("Rose",      0xFFAD1457, 0xFF6A1B9A),
        GradientPreset("Fire",      0xFFFF9500, 0xFFC62828),
        GradientPreset("Ice",       0xFF00838F, 0xFF1E88E5),
        GradientPreset("Night",     0xFF0A0A0A, 0xFF1A1A1A),
        GradientPreset("Gold",      0xFFF9A825, 0xFFE65100),
        GradientPreset("Cotton",    0xFFFFB6C1, 0xFF87CEEB),
        GradientPreset("Mint",      0xFF34C759, 0xFF00838F),
        GradientPreset("Steel",     0xFF2C2C2E, 0xFF0A0A0A),
    )

    data class GradientPreset(
        val name: String,
        val colorTop: Long,
        val colorBottom: Long
    )

    // ============================================
    // SHARED PREFERENCES
    // ============================================
    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // ============ GLASS ALPHA ============
    fun getGlassAlpha(context: Context): Float {
        return prefs(context).getFloat(KEY_GLASS_ALPHA, DEFAULT_GLASS_ALPHA)
    }

    fun setGlassAlpha(context: Context, value: Float) {
        prefs(context).edit().putFloat(KEY_GLASS_ALPHA, value.coerceIn(0.1f, 1f)).apply()
    }

    // ============ BORDER ALPHA ============
    fun getBorderAlpha(context: Context): Float {
        return prefs(context).getFloat(KEY_BORDER_ALPHA, DEFAULT_BORDER_ALPHA)
    }

    fun setBorderAlpha(context: Context, value: Float) {
        prefs(context).edit().putFloat(KEY_BORDER_ALPHA, value.coerceIn(0f, 1f)).apply()
    }

    // ============ TOP COLOR ============
    fun getIslandColor(context: Context): Long {
        return prefs(context).getLong(KEY_ISLAND_COLOR, DEFAULT_COLOR)
    }

    fun setIslandColor(context: Context, color: Long) {
        prefs(context).edit().putLong(KEY_ISLAND_COLOR, color).apply()
    }

    // ============ BOTTOM COLOR ============
    fun getIslandColorBottom(context: Context): Long {
        return prefs(context).getLong(KEY_ISLAND_COLOR_BOTTOM, DEFAULT_COLOR)
    }

    fun setIslandColorBottom(context: Context, color: Long) {
        prefs(context).edit().putLong(KEY_ISLAND_COLOR_BOTTOM, color).apply()
    }

    // ============ USE GRADIENT ============
    fun getUseGradient(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_USE_GRADIENT, false)
    }

    fun setUseGradient(context: Context, use: Boolean) {
        prefs(context).edit().putBoolean(KEY_USE_GRADIENT, use).apply()
    }

    // ============ RESET ============
    fun resetAll(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
