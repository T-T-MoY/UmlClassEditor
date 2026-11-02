package com.nathaniel.motus.umlclasseditor

import android.app.Application
import com.nathaniel.motus.umlclasseditor.controller.ThemeManager

/**
 * Punto de entrada de la aplicación.
 *
 * Aplica el tema (claro / oscuro / sistema) antes de que se cree cualquier
 * Activity, para evitar un parpadeo con el tema equivocado al arrancar.
 */
class UmlClassEditorApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ThemeManager.applySavedTheme(this)
    }
}
