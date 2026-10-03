package ru.devcustrom.androidlab

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import ru.devcustrom.androidlab.common.LabAdapter
import ru.devcustrom.androidlab.common.LabCatalog
import ru.devcustrom.androidlab.common.LabIntents
import ru.devcustrom.androidlab.common.LabItem
import ru.devcustrom.androidlab.common.StudentPrefs
import ru.devcustrom.androidlab.databinding.ActivityMainBinding
import ru.devcustrom.androidlab.lab3.AboutActivity
import ru.devcustrom.androidlab.lab3.SettingsActivity

/**
 * Лабораторная 3: главный экран со списком всех работ.
 *
 * Здесь учатся три вещи:
 * - **options-меню** — три точки в тулбаре ([onCreateOptionsMenu]);
 * - **контекстное меню** — долгое нажатие по пункту ([onCreateContextMenu]);
 * - **обмен данными** — `Intent.putExtra` наружу и `ActivityResult` обратно.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: StudentPrefs

    private val adapter = LabAdapter(
        onClick = ::openLab,
        onLongClick = ::showLabMenu,
    )

    /**
     * Лаба 1 возвращает результат через [ActivityResultContracts.StartActivityForResult].
     *
     * Регистрация происходит **до** `STARTED` — если зарегистрировать позже,
     * Android выбросит `IllegalStateException`, потому что результат может прийти
     * раньше, чем Activity успеет подписаться.
     */
    private val lab1Launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val lastValue = result.data?.getStringExtra(LabIntents.EXTRA_LAST_VALUE)
        if (result.resultCode == RESULT_OK && !lastValue.isNullOrBlank()) {
            Log.i(TAG, "Lab1Activity вернула: $lastValue")
            Snackbar.make(
                binding.root,
                getString(R.string.main_last_value, lastValue),
                Snackbar.LENGTH_LONG,
            ).show()
        } else {
            Log.i(TAG, "Lab1Activity ничего не вернула: code=${result.resultCode}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = StudentPrefs(applicationContext)

        // Тема — NoActionBar, поэтому тулбар из разметки сам по себе не является
        // action bar, и `onCreateOptionsMenu` **не вызывается**: кнопки «три
        // точки» просто не существует, а `menuInflater.inflate` молча ничего
        // не делает. Связь создаёт именно этот вызов.
        setSupportActionBar(binding.toolbar)

        binding.labsList.layoutManager = LinearLayoutManager(this)
        binding.labsList.adapter = adapter
        adapter.submitList(LabCatalog.all)
    }

    // ---------------------------------------------------------------- options-меню

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_about -> {
            startActivity(aboutIntent())
            true
        }

        R.id.action_settings -> {
            startActivity(Intent(this, SettingsActivity::class.java))
            true
        }

        R.id.action_exit -> {
            // finishAffinity() закрывает сразу все экраны этого приложения,
            // а не только MainActivity, к которому мы возвращались по стрелке.
            finishAffinity()
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    // ------------------------------------------------------------ контекст-меню

    /**
     * Контекстное меню по долгому нажатию.
     *
     * Сделано через [PopupMenu], а не `registerForContextMenu(labsList)`, и это
     * не украшение: у `RecyclerView` **нет** `getContextMenuInfo()`, поэтому в
     * `onCreateContextMenu` приходит `info == null`, и позицию нажатой строки
     * взять неоткуда. Список позиций приходилось бы угадывать. Адаптер знает
     * позицию точно (`bindingAdapterPosition`) и передаёт её вместе с `View`-
     * якорем, на котором Android и покажет меню.
     */
    private fun showLabMenu(item: LabItem, anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.menu_lab_item, popup.menu)

        // `PopupMenu.getMenu()` отдаёт `Menu`, а `setHeaderTitle` живёт только на
        // `ContextMenu`. Внутри там лежит `MenuBuilder`, который `ContextMenu`
        // реализует, поэтому каст проходит; `as?` — чтобы не упасть, если
        // Android когда-нибудь поменяет внутренности.
        (popup.menu as? ContextMenu)?.setHeaderTitle(getString(item.titleRes))

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_open ->
                    if (item.isAvailable) openLab(item) else notifyNotReady(item)

                R.id.action_share -> shareLab(item)
            }
            true
        }

        popup.show()
    }

    // ------------------------------------------------------------------ действия

    private fun openLab(item: LabItem) {
        val target = item.activityClass ?: return notifyNotReady(item)

        if (item.number == 1) {
            lab1Launcher.launch(labIntent(target))
        } else {
            startActivity(labIntent(target))
        }
    }

    /**
     * `Intent` для любой лабы. Имя студента едет вместе с запросом — так его
     * получает любой будущий экран, без правок в этом файле.
     */
    private fun labIntent(target: Class<out Activity>): Intent =
        Intent(this, target).apply {
            putExtra(LabIntents.EXTRA_STUDENT_NAME, prefs.studentName)
        }

    private fun aboutIntent(): Intent =
        Intent(this, AboutActivity::class.java).apply {
            putExtra(LabIntents.EXTRA_STUDENT_NAME, prefs.studentName)
        }

    private fun shareLab(item: LabItem) {
        val text = getString(R.string.main_share_format, getString(item.titleRes))
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        try {
            startActivity(Intent.createChooser(send, getString(R.string.main_share_chooser)))
        } catch (e: ActivityNotFoundException) {
            Snackbar.make(binding.root, R.string.main_no_share_app, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun notifyNotReady(item: LabItem) {
        Snackbar.make(
            binding.root,
            getString(R.string.main_not_ready, getString(item.titleRes)),
            Snackbar.LENGTH_SHORT,
        ).show()
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}