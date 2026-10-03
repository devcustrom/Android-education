package ru.devcustrom.androidlab.lab3

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ru.devcustrom.androidlab.R
import ru.devcustrom.androidlab.common.LabIntents
import ru.devcustrom.androidlab.databinding.ActivityAboutBinding

/**
 * Лабораторная 3: экран «О программе».
 *
 * Сюда данные приходят через `Intent`: [LabIntents.EXTRA_STUDENT_NAME] кладёт в
 * него `MainActivity`, а здесь мы читаем через `getStringExtra`.
 *
 * Экран намеренно **не** переживает поворот через `Bundle` и не читает
 * `SharedPreferences`: он должен показать, что `Intent` — это «одноразовый
 * конверт» между двумя конкретными запусками.
 */
class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        val studentName = intent.getStringExtra(LabIntents.EXTRA_STUDENT_NAME).orEmpty()

        binding.studentNameText.text = if (studentName.isBlank()) {
            getString(R.string.about_student_unset)
        } else {
            getString(R.string.about_student_format, studentName)
        }

        binding.versionText.text = getString(R.string.about_version_format, versionName())

        binding.shareButton.setOnClickListener { shareApp() }
    }

    /**
     * Версия из установленного пакета, а не из константы в коде.
     *
     * `BuildConfig` для этого пришлось бы включать флагом `buildFeatures.buildConfig`
     * в Gradle, а версия всё равно лежит в том же месте — в манифесте, из которого
     * Android и собирает `PackageInfo`.
     */
    private fun versionName(): String = runCatching {
        packageManager.getPackageInfo(packageName, 0).versionName
    }.getOrNull() ?: "—"

    private fun shareApp() {
        val text = getString(R.string.about_share_text)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        // chooser: системный выбор приложения, без него Android может показать
        // «не найдено приложение» на устройствах, где нет дефолтного получателя.
        startActivity(Intent.createChooser(intent, getString(R.string.about_share_chooser)))
    }
}