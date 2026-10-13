package ru.devcustrom.androidlab.lab3

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import ru.devcustrom.androidlab.R
import ru.devcustrom.androidlab.common.StudentPrefs
import ru.devcustrom.androidlab.databinding.ActivitySettingsBinding

/**
 * Экран «Настройки» из options-меню главного экрана.
 *
 * В нём два блока:
 * - имя студента — чтобы следующая лаба могла показать `Intent.putExtra`;
 * - выбор языка — прикладная локализация Лабы 6 через
 *   [AppCompatDelegate.setApplicationLocales]. Строки берутся из `values-en`
 *   и `values-de`, поэтому переключение видно сразу, без пересборки.
 *
 * Экрана «Настройки» в плане нет, но пункт меню «Настройки» обязан что-то
 * открывать: мёртвый пункт в меню — это баг, а не заглушка.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: StudentPrefs

    /** `null` в теге — «как в системе» (пустой список локалей). */
    private val languageOptions: List<Pair<String?, Int>> = listOf(
        null to R.string.lab6_language_system,
        "ru" to R.string.lab6_language_ru,
        "en" to R.string.lab6_language_en,
        "de" to R.string.lab6_language_de,
    )

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
        binding.languageButton.setOnClickListener { showLanguagePicker() }
        renderLanguageButton()
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

    // ------------------------------------------------------------------- язык

    private fun currentLanguageTag(): String? {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) null else locales.toLanguageTags().substringBefore(',')
    }

    private fun renderLanguageButton() {
        val tag = currentLanguageTag()
        val labelRes = languageOptions.firstOrNull { it.first == tag }?.second
            ?: R.string.lab6_language_system
        binding.languageButton.text = getString(labelRes)
    }

    private fun showLanguagePicker() {
        val current = currentLanguageTag()
        val labels = languageOptions.map { getString(it.second) }.toTypedArray()
        val checked = languageOptions.indexOfFirst { it.first == current }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.lab6_choose_language)
            .setSingleChoiceItems(labels, checked) { dialog, which ->
                applyLanguage(languageOptions[which].first)
                dialog.dismiss()
            }
            .show()
    }

    /**
     * `AppCompatDelegate.setApplicationLocales` — правильный способ сменить язык
     * приложения: он не трогает системную локаль, применяется на всех версиях
     * (через AppCompat на API < 33) и переживает перезапуск, если в манифесте
     * объявлен `AppLocalesMetadataHolderService` с `autoStoreLocales`.
     */
    private fun applyLanguage(tag: String?) {
        val locales = if (tag == null) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
        // AppCompat сам пересоздаёт Activity, но подстраховка не мешает:
        // кнопка с текущим языком должна перерисоваться в новом языке.
        recreate()
    }

    private companion object {
        const val STATE_NAME = "student_name"
    }
}
