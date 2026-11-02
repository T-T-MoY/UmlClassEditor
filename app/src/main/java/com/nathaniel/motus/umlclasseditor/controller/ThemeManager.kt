package com.nathaniel.motus.umlclasseditor.controller

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/**
 * Centraliza la selección de tema de la aplicación (claro / oscuro / sistema).
 *
 * El modo elegido se guarda en SharedPreferences y se aplica con
 * [AppCompatDelegate.setDefaultNightMode], de modo que los recursos de
 * `values` y `values-night` se resuelvan correctamente en toda la app.
 */
object ThemeManager {

    const val MODE_SYSTEM = 0
    const val MODE_LIGHT = 1
    const val MODE_DARK = 2

    private const val PREFERENCES_NAME = "theme_settings"
    private const val KEY_THEME_MODE = "theme_mode"

    /** Devuelve el modo guardado, o [MODE_SYSTEM] si el usuario aún no eligió nada. */
    fun getSavedMode(context: Context): Int =
        preferences(context).getInt(KEY_THEME_MODE, MODE_SYSTEM)

    /** Aplica el tema guardado. Debe llamarse antes de crear cualquier Activity. */
    fun applySavedTheme(context: Context) {
        AppCompatDelegate.setDefaultNightMode(toNightMode(getSavedMode(context)))
    }

    /** Guarda el modo elegido y lo aplica en toda la aplicación. */
    fun setMode(context: Context, mode: Int) {
        preferences(context).edit().putInt(KEY_THEME_MODE, mode).apply()
        AppCompatDelegate.setDefaultNightMode(toNightMode(mode))
    }

    private fun toNightMode(mode: Int): Int = when (mode) {
        MODE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        MODE_DARK -> AppCompatDelegate.MODE_NIGHT_YES
        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
}
