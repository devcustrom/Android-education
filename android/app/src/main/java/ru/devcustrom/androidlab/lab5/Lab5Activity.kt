package ru.devcustrom.androidlab.lab5

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import ru.devcustrom.androidlab.R
import ru.devcustrom.androidlab.data.db.Note
import ru.devcustrom.androidlab.data.repository.NotesRepository
import ru.devcustrom.androidlab.databinding.ActivityLab5Binding
import ru.devcustrom.androidlab.ui.NotesUiState
import ru.devcustrom.androidlab.ui.NotesViewModel
import kotlinx.coroutines.launch

/**
 * Лаба 5: заметки на Room.
 *
 * Экран **ничего не загружает и не сохраняет сам**. Он подписан на
 * [NotesViewModel] и рисует то, что пришло. Поэтому удаление заметки в
 * `NoteEditActivity` обновляет этот список без единого обратного вызова:
 * база сообщила о изменении, `Flow` принёс новое значение, список перерисовался.
 *
 * ```
 *  DAO: INSERT/UPDATE/DELETE
 *       │
 *       ▼  Room следит за таблицей
 *  Flow<List<Note>>  ──►  ViewModel.stateIn  ──►  collect  ──►  submitList
 * ```
 *
 * Именно поэтому здесь нет ни `notifyDataSetChanged`, ни ручного обновления
 * после сохранения: обновлять нечего, данные меняются сами.
 */
class Lab5Activity : AppCompatActivity() {

    private lateinit var binding: ActivityLab5Binding

    private val viewModel: NotesViewModel by viewModels {
        NotesViewModel.Factory(NotesRepository(applicationContext))
    }

    private val adapter = NoteAdapter(onClick = ::openNote)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLab5Binding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.inflateMenu(R.menu.menu_notes)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_delete_all -> { confirmDeleteAll(); true }
                else -> false
            }
        }

        // Поиск. `onQueryTextChange` срабатывает на каждый символ — именно
        // поэтому во ViewModel стоит debounce, а запрос к базе делается один раз
        // после паузы, а не десять.
        val searchView = binding.toolbar.menu
            .findItem(R.id.action_search)
            .actionView as SearchView
        searchView.queryHint = getString(R.string.lab5_search_hint)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.onQueryChanged(query.orEmpty())
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.onQueryChanged(newText.orEmpty())
                return true
            }
        })

        binding.notesList.layoutManager = LinearLayoutManager(this)
        binding.notesList.adapter = adapter
        attachSwipeToDelete()

        binding.addButton.setOnClickListener {
            startActivity(NoteEditActivity.intent(this, noteId = 0L))
        }

        // Подписка живёт ровно пока Activity видима: collect наSTARTED–STOPPED,
        // чтобы в фоне не крутился цикл на пустом экране.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: NotesUiState) = with(binding) {
        // Счётчик обновляется для всех состояний, а не только для списка.
        // Иначе после удаления последней заметки в тулбаре навсегда осталось бы
        // «3 заметки», хотя список пуст.
        toolbar.subtitle = when (state) {
            is NotesUiState.Notes ->
                resources.getQuantityString(R.plurals.lab5_count, state.list.size, state.list.size)

            else -> null
        }

        when (state) {
            NotesUiState.Loading -> {
                notesList.visibility = View.GONE
                emptyView.visibility = View.GONE
                progress.visibility = View.VISIBLE
            }

            NotesUiState.Empty -> {
                progress.visibility = View.GONE
                notesList.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
                emptyView.setText(
                    if (viewModel.searchQuery.value.isEmpty()) {
                        R.string.lab5_empty
                    } else {
                        R.string.lab5_empty_search
                    },
                )
            }

            is NotesUiState.Notes -> {
                progress.visibility = View.GONE
                emptyView.visibility = View.GONE
                notesList.visibility = View.VISIBLE
                adapter.submitList(state.list)
            }
        }
    }

    private fun openNote(note: Note) {
        startActivity(NoteEditActivity.intent(this, note.id))
    }

    /**
     * Удаление смахом.
     *
     * Сам список не трогаем: `viewModel.delete()` пишет в базу, а новое
     * состояние приезжает тем же `Flow`, что и всё остальное. Snackbar с
     * отменой удаляет этот путь вообще: заметка возвращается тем же `id`.
     */
    private fun attachSwipeToDelete() {
        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder,
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val note = adapter.currentList.getOrNull(position) ?: return
                viewModel.delete(note)
                Snackbar.make(binding.root, R.string.lab5_deleted, Snackbar.LENGTH_LONG)
                    .setAction(R.string.lab5_undo) { viewModel.restore(note) }
                    .show()
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(binding.notesList)
    }

    /**
     * Удаление всех заметок спрашивает подтверждение: восстановить их нечем,
     * это локальная база без синхронизации.
     */
    private fun confirmDeleteAll() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.lab5_delete_all)
            .setMessage(R.string.lab5_delete_all_confirm)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.lab5_delete) { _, _ ->
                viewModel.deleteAll()
                Snackbar.make(binding.root, R.string.lab5_all_deleted, Snackbar.LENGTH_SHORT).show()
            }
            .show()
    }
}