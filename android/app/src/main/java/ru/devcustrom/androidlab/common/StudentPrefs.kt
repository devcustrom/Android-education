package ru.devcustrom.androidlab.common

import android.content.Context
import android.content.SharedPreferences

/**
 * Имя студента, которое передаётся между экранами через `Intent`.
 *
 * Источник — `SharedPreferences`, потому что выбор должен переживать перезапуск
 * приложения, а `Bundle` живёт только до поворота экрана.
 */
class StudentPrefs(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    var studentName: String
        get() = prefs.getString(KEY_NAME, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_NAME, value.trim()).apply()

    private companion object {
        const val FILE_NAME = "settings"
        const val KEY_NAME = "student_name"
    }
}