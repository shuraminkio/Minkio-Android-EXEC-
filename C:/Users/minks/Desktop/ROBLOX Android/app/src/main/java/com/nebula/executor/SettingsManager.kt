package com.nebula.executor

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("nebula_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_AUTO_EXECUTE = "auto_execute"
        private const val KEY_SHOW_TIMESTAMPS = "show_timestamps"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_AUTO_INJECT = "auto_inject"
        private const val KEY_TOPMOST = "topmost"

        private const val DEFAULT_FONT_SIZE = 13
    }

    var autoExecute: Boolean
        get() = prefs.getBoolean(KEY_AUTO_EXECUTE, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_EXECUTE, value).apply()

    var showTimestamps: Boolean
        get() = prefs.getBoolean(KEY_SHOW_TIMESTAMPS, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_TIMESTAMPS, value).apply()

    var fontSize: Int
        get() = prefs.getInt(KEY_FONT_SIZE, DEFAULT_FONT_SIZE)
        set(value) = prefs.edit().putInt(KEY_FONT_SIZE, value).apply()

    var autoInject: Boolean
        get() = prefs.getBoolean(KEY_AUTO_INJECT, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_INJECT, value).apply()

    var topmost: Boolean
        get() = prefs.getBoolean(KEY_TOPMOST, true)
        set(value) = prefs.edit().putBoolean(KEY_TOPMOST, value).apply()
}
