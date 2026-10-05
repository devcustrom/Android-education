package ru.devcustrom.androidlab.data.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import ru.devcustrom.androidlab.data.db.AppDatabase
import ru.devcustrom.androidlab.data.db.Note

/**
 * Обёртка над [ru.devcustrom.androidlab.data.db.NoteDao].
 *
 * Слой существует ради одной вещи: чтобы `ViewModel` и экраны не знали, откуда
 * взялись заметки. Сегодня это Room, завтра может быть API с кэшем в Room —
 * и экраны не изменятся.
 *
 * Заодно здесь живёт «бизнес-правило» валидации. Проверять заголовок в
 * Activity плохо: правило разъедется по кодовой базе, и Lab 6 не сможет
 * создать заметку из виджета по тем же правилам.
 */
class NotesRepository(context: Context) {

    private val dao = AppDatabase.get(context).noteDao()

    fun observeNotes(): Flow<List<Note>> = dao.observeAll()

    fun observeCount(): Flow<Int> = dao.observeCount()

    fun search(query: String): Flow<List<Note>> = dao.search(query)

    suspend fun findById(id: Long): Note? = dao.findById(id)

    suspend fun count(): Int = dao.count()

    /**
     * Сохраняет заметку: создаёт новую или обновляет существующую.
     *
     * Разделение на `id == 0L` → `insert` иначе `update` выбрано сознательно:
     * вызывающий код не обязан знать, новая это заметка или старая. Один
     * метод «сохранить» вместо двух «создать» и «изменить» — меньше мест,
     * где можно забыть про `updatedAt`.
     *
     * @return `id` заметки после сохранения
     */
    suspend fun save(note: Note, now: Long): Long = if (note.id == NEW_NOTE) {
        dao.insert(note.copy(createdAt = note.createdAt.takeIf { it > 0 } ?: now, updatedAt = now))
    } else {
        dao.update(note.copy(updatedAt = now))
        note.id
    }

    suspend fun delete(note: Note) = dao.delete(note)

    /**
     * Возвращает удалённую заметку.
     *
     * Отдельный метод, а не [save], потому что `save` для заметки с `id != 0`
     * делает `@Update` — а `@Update` ищет строку по первичному ключу и, не
     * найдя её, **молча** обновляет ноль строк. Вызов выглядит успешным,
     * заметка не возвращается, и ошибку видно только глазами на экране.
     *
     * `@Insert(onConflict = REPLACE)` с уже существующим `id` восстанавливает
     * строку как есть. Это работает и для создания, и для возврата — тот же
     * приём, что и `INSERT OR REPLACE` в SQL.
     */
    suspend fun restore(note: Note, now: Long): Long =
        dao.insert(note.copy(updatedAt = now))

    suspend fun deleteAll() = dao.deleteAll()

    companion object {
        /** `id = 0` — соглашение Room о «ещё не сохранено». */
        const val NEW_NOTE = 0L
    }
}