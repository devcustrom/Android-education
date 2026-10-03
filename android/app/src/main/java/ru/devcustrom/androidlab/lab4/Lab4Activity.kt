package ru.devcustrom.androidlab.lab4

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ContextMenu
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.devcustrom.androidlab.R
import ru.devcustrom.androidlab.data.model.Cat
import ru.devcustrom.androidlab.data.repository.CatRepository
import ru.devcustrom.androidlab.databinding.ActivityLab4Binding

/**
 * Лаба 4: работа с файлами и сохранение состояния.
 *
 * Экран сознательно **не ходит в сеть**. Его источник данных — только кэш
 * `cats_cache.json`, который остался после Лабы 2. Так видна разница: сеть живёт
 * секунды, а файл живёт до очистки данных приложения.
 *
 * Что здесь демонстрируется:
 * * чтение файла из `filesDir` без единого разрешения;
 * * [`onSaveInstanceState`] — состояние переживает поворот экрана;
 * * экспорт в «Загрузки»: `MediaStore` на API 29+ и каталог приложения раньше;
 * * контекстное меню на элементе списка.
 */
class Lab4Activity : AppCompatActivity() {

    private lateinit var binding: ActivityLab4Binding
    private val repository by lazy { CatRepository(applicationContext) }

    private val cacheAdapter: CacheCatAdapter = CacheCatAdapter(
        onToggle = ::toggleCat,
        onLongPress = ::showCatMenu,
    )

    private fun toggleCat(cat: Cat) {
        cacheAdapter.toggleSelection(cat.id)
        updateSelectedInfo()
    }

    /** Что восстанавливаем: позиция скролла и отмеченные котики. */
    private var pendingScrollPosition = 0
    private var pendingSelectedIds: Set<String> = emptySet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLab4Binding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.catsList.layoutManager = LinearLayoutManager(this)
        binding.catsList.adapter = cacheAdapter

        binding.viewCacheButton.setOnClickListener { openCacheView() }
        binding.clearCacheButton.setOnClickListener { clearCache() }
        binding.exportButton.setOnClickListener { exportCache() }

        // Bundle приходит сюда и при повороте из процесса, и при обычном
        // восстановлении после onSaveInstanceState. Поэтому состояние читаем
        // здесь, а применяем — в reload(), когда список уже готов.
        if (savedInstanceState != null) restoreFrom(savedInstanceState)

        reload()
    }

    // region состояние экрана

    /**
     * Вызывается системой перед тем, как Activity уйдёт в фон или будет
     * уничтожена.
     *
     * Сюда кладут **только** то, что дёшево собрать и дёшево вернуть: числа,
     * строки, `id` выбранных элементов. Сюда не кладут bitmap-ы, списки из сети и
     * большие `Bundle`-ы — система хранит их в памяти процесса, а при нехватке
     * памяти сериализует на диск, и тогда большой `Bundle` превращается в
     * `TransactionTooLargeException`.
     *
     * Самое частое заблуждение: «состояние и так сохранится, зачем это делать».
     * Не сохранится. Android пересоздаёт Activity при повороте, и без этого
     * метода пользователь теряет позицию скролла и все отметки.
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val first = (binding.catsList.layoutManager as? LinearLayoutManager)
            ?.findFirstVisibleItemPosition() ?: 0
        outState.putInt(STATE_SCROLL, first)
        outState.putStringArrayList(STATE_SELECTED, ArrayList(cacheAdapter.selected))
        Log.i(TAG, "onSaveInstanceState: scroll=$first, выбрано=${cacheAdapter.selected.size}")
    }

    /**
     * Вызывается **только** если Activity была уничтожена и создана заново
     * (поворот, нехватка памяти). При простом возврате из фона не вызывается —
     * поэтому полагаться на него как на «сохранение состояния» нельзя.
     */
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        restoreFrom(savedInstanceState)
        reload()
    }

    private fun restoreFrom(state: Bundle) {
        pendingScrollPosition = state.getInt(STATE_SCROLL, 0)
        pendingSelectedIds = state.getStringArrayList(STATE_SELECTED).orEmpty().toSet()
    }

    // endregion

    // region данные

    /** Перечитывает кэш с диска. Не из памяти — так видно, что файл правда есть. */
    private fun reload() {
        lifecycleScope.launch {
            val cached = withContext(Dispatchers.IO) { repository.cache.read() }
            val cats = cached.orEmpty()

            val aliveIds = cats.map { it.id }.toSet()
            val stillSelected = cacheAdapter.selected intersect aliveIds
            val selection = if (pendingSelectedIds.isNotEmpty()) {
                pendingSelectedIds intersect aliveIds
            } else {
                stillSelected
            }

            cacheAdapter.submitList(cats) { restoreScroll() }
            cacheAdapter.restoreSelection(selection)

            binding.cacheInfo.text = if (repository.cache.exists()) {
                getString(R.string.lab4_cache_info, cats.size, repository.cache.sizeBytes())
            } else {
                getString(R.string.lab4_cache_missing)
            }
            binding.cachePath.text = repository.cache.path
            binding.emptyView.visibility =
                if (cats.isEmpty()) View.VISIBLE else View.GONE
            updateSelectedInfo()
        }
    }

    private fun restoreScroll() {
        if (pendingScrollPosition <= 0) return
        val count = cacheAdapter.itemCount
        if (count == 0) return
        (binding.catsList.layoutManager as? LinearLayoutManager)
            ?.scrollToPositionWithOffset(pendingScrollPosition.coerceAtMost(count - 1), 0)
    }

    private fun updateSelectedInfo() {
        binding.selectedInfo.text = getString(R.string.lab4_selected, cacheAdapter.selected.size)
    }

    private fun openCacheView() {
        if (!repository.cache.exists()) {
            binding.cacheInfo.setText(R.string.lab4_cache_missing)
            return
        }
        startActivity(CacheViewActivity.intent(this))
    }

    private fun clearCache() {
        lifecycleScope.launch {
            val deleted = withContext(Dispatchers.IO) { repository.cache.clear() }
            reload()
            binding.cacheInfo.setText(
                if (deleted) R.string.lab4_cache_cleared else R.string.lab4_cache_clear_failed,
            )
        }
    }

    private fun exportCache() {
        binding.exportButton.isEnabled = false
        lifecycleScope.launch {
            val result = CacheExporter.export(this@Lab4Activity, repository.cache.sourceFile, EXPORT_NAME)
            binding.exportButton.isEnabled = true
            when (result) {
                is CacheExporter.Result.Success -> {
                    Log.i(TAG, "Экспорт: ${result.location}")
                    binding.cacheInfo.text = getString(R.string.lab4_export_ok, result.location)
                }

                is CacheExporter.Result.Failure ->
                    binding.cacheInfo.text = getString(R.string.lab4_export_failed, result.message)
            }
        }
    }

    // endregion

    // region контекстное меню

    /** Долгое нажатие на кота: три действия над элементом списка. */
    private fun showCatMenu(cat: Cat, anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.menu_cache_cat, popup.menu)
        (popup.menu as? ContextMenu)?.setHeaderTitle(cat.id)
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_share_cat -> { shareCat(cat); true }

                R.id.action_remove_cat -> { removeFromCache(cat); true }

                R.id.action_copy_cat_id -> { copyCatId(cat); true }

                else -> false
            }
        }
        popup.show()
    }

    private fun shareCat(cat: Cat) {
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "${cat.imageUrl}\n${cat.tagLabel}")
                },
                getString(R.string.lab4_share_cat),
            ),
        )
    }

    private fun removeFromCache(cat: Cat) {
        lifecycleScope.launch {
            val removed = withContext(Dispatchers.IO) { repository.cache.remove(cat.id) }
            Log.i(TAG, "Удалён из кэша ${cat.id}: $removed")
            reload()
            binding.cacheInfo.text = getString(
                if (removed) R.string.lab4_cat_removed else R.string.lab4_cat_not_found,
                cat.id,
            )
        }
    }

    private fun copyCatId(cat: Cat) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("cat id", cat.id))
        binding.selectedInfo.text = getString(R.string.lab4_id_copied, cat.id)
    }

    // endregion

    private companion object {
        const val TAG = "Lab4Activity"
        const val STATE_SCROLL = "state_scroll"
        const val STATE_SELECTED = "state_selected"
        const val EXPORT_NAME = "cats_cache.json"
    }
}