package ru.devcustrom.androidlab.lab5

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import ru.devcustrom.androidlab.R
import ru.devcustrom.androidlab.data.db.Note
import ru.devcustrom.androidlab.data.repository.NotesRepository
import ru.devcustrom.androidlab.databinding.ActivityNoteEditBinding

/**
 * Создание и редактирование заметки.
 *
 * Один экран на два сценария — признак `id == 0`. В плане они описаны как
 * `lab5/NoteEditActivity`, и это правильно: поля, заголовок и кнопка «Сохранить»
 * у них одинаковые, различается только то, откуда взялся `id`.
 *
 * ```
 *  id = 0                 id > 0
 *  ─────────              ─────────
 *  новый Note             findById(id) → подставить в поля
 *  save() → insert        save()      → update
 *  title = ввод           title = из базы
 * ```
 */
class NoteEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoteEditBinding
    private lateinit var repository: NotesRepository

    private var noteId: Long = 0L
    private var loaded: Note? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        repository = NotesRepository(applicationContext)

        noteId = intent.getLongExtra(EXTRA_NOTE_ID, NotesRepository.NEW_NOTE)
        binding.toolbar.setTitle(
            if (noteId == NotesRepository.NEW_NOTE) R.string.lab5_new_note else R.string.lab5_edit_note,
        )

        if (noteId != NotesRepository.NEW_NOTE) loadNote()

        binding.saveButton.setOnClickListener { save() }
    }

    /**
     * Загрузка заметки — `suspend`, потому что это запрос к базе.
     *
     * `lifecycleScope.launch` отменяется, если Activity уйдёт с экрана, и
     * результат не придёт в уже мёртвый экран. Ручной `Thread` здесь был бы
     * источником `IllegalStateException: Fragment/Activity not attached`.
     */
    private fun loadNote() {
        lifecycleScope.launch {
            val note = repository.findById(noteId)
            if (note == null) {
                // Заметку удалили на другом экране, пока этот был открыт.
                Snackbar.make(binding.root, R.string.lab5_note_missing, Snackbar.LENGTH_LONG).show()
                finish()
                return@launch
            }
            loaded = note
            binding.titleInput.setText(note.title)
            binding.bodyInput.setText(note.body)
        }
    }

    private fun save() {
        val title = binding.titleInput.text?.toString()?.trim().orEmpty()
        val body = binding.bodyInput.text?.toString()?.trim().orEmpty()

        if (title.isEmpty() && body.isEmpty()) {
            binding.titleLayout.error = getString(R.string.lab5_empty_note)
            return
        }
        binding.titleLayout.error = null

        val now = System.currentTimeMillis()
        val existing = loaded
        val note = existing?.copy(title = title, body = body)
            ?: Note(title = title, body = body, createdAt = now, updatedAt = now)

        binding.saveButton.isEnabled = false
        lifecycleScope.launch {
            repository.save(note, now = now)
            Snackbar.make(binding.root, R.string.lab5_saved, Snackbar.LENGTH_SHORT).show()
            finish()
        }
    }

    companion object {
        private const val EXTRA_NOTE_ID = "note_id"

        fun intent(context: Context, noteId: Long): Intent =
            Intent(context, NoteEditActivity::class.java).putExtra(EXTRA_NOTE_ID, noteId)
    }
}