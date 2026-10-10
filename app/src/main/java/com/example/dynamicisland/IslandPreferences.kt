package com.example.dynamicisland

import android.content.Context
import android.content.SharedPreferences

/**
 * Simpan preferensi user (transparansi island, dll).
 */
object IslandPreferences {

    private const val PREF_NAME = "island_prefs"
    private const val KEY_GLASS_ALPHA = "glass_alpha"
    private const val KEY_BORDER_ALPHA = "border_alpha"

    // Default: 67% (semi-transparan)
    const val DEFAULT_GLASS_ALPHA = 0.67f
    const val DEFAULT_BORDER_ALPHA = 0.15f

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getGlassAlpha(context: Context): Float {
        return prefs(context).getFloat(KEY_GLASS_ALPHA, DEFAULT_GLASS_ALPHA)
    }

    fun setGlassAlpha(context: Context, value: Float) {
        prefs(context).edit().putFloat(KEY_GLASS_ALPHA, value.coerceIn(0.1f, 1f)).apply()
    }

    fun getBorderAlpha(context: Context): Float {
        return prefs(context).getFloat(KEY_BORDER_ALPHA, DEFAULT_BORDER_ALPHA)
    }

    fun setBorderAlpha(context: Context, value: Float) {
        prefs(context).edit().putFloat(KEY_BORDER_ALPHA, value.coerceIn(0f, 1f)).apply()
    }
}
