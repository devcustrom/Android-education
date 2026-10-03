package ru.devcustrom.androidlab.lab3

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import ru.devcustrom.androidlab.R
import ru.devcustrom.androidlab.common.StudentPrefs
import ru.devcustrom.androidlab.databinding.ActivitySettingsBinding

/**
 * Экран «Настройки» из options-меню главного экрана.
 *
 * В нём всего одно поле — имя студента. Оно нужно, чтобы следующая лаба могла
 * показать, как `MainActivity` передаёт данные в другие экраны через
 * `Intent.putExtra("student_name", ...)`.
 *
 * Экрана «Настройки» в плане нет, но пункт меню «Настройки» обязан что-то
 * открывать: мёртвый пункт в меню — это баг, а не заглушка.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: StudentPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = StudentPrefs(applicationContext)

        binding.toolbar.setNavigationOnClickListener { finish() }

        // `savedInstanceState` важнее prefs: пользователь мог уже отредактировать
        // поле в этой сессии, но ещё не нажать «Сохранить».
        binding.nameInput.setText(
            savedInstanceState?.getString(STATE_NAME) ?: prefs.studentName
        )

        binding.saveButton.setOnClickListener { save() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_NAME, binding.nameInput.text?.toString().orEmpty())
    }

    private fun save() {
        val name = binding.nameInput.text?.toString().orEmpty().trim()

        if (name.isBlank()) {
            binding.nameLayout.error = getString(R.string.settings_name_error)
            return
        }

        binding.nameLayout.error = null
        prefs.studentName = name

        Snackbar.make(binding.root, R.string.settings_saved, Snackbar.LENGTH_SHORT).show()
    }

    private companion object {
        const val STATE_NAME = "student_name"
    }
}