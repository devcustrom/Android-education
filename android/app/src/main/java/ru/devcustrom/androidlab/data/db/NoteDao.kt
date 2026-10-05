package ru.devcustrom.androidlab.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Запросы к таблице `notes`.
 *
 * Разница между двумя видами методов — главное, что стоит понять в Room:
 *
 * ```
 *  suspend fun insert(note)      →  один раз, ждём результат, пишем
 *  fun observeAll(): Flow<…>     →  подписка, приходит новое при каждом
 *                                   изменении таблицы, отписка — сама
 * ```
 *
 * Если написать `suspend fun getAll(): List<Note>` и вызвать её из Activity,
 * список будет одноразовым: он не обновится, когда заметку добавят на другом
 * экране. `Flow` — это не «удобная асинхронность», а **подписка на изменения
 * таблицы**, которую Room поддерживает из коробки.
 *
 * `suspend` в методах не декоративен: Room сам переносит их на свой
 * `queryExecutor`, поэтому `Dispatchers.IO` в вызывающем коде не нужен.
 */
@Dao
interface NoteDao {

    /**
     * Подписка на весь список. Сортировка по `updatedAt` — тут же в SQL,
     * а не через `sortedBy` в Kotlin: база всё равно читает таблицу целиком,
     * но из индекса, и не держит в памяти лишнюю копию.
     */
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<Note>>

    /** Количество заметок — отдельным `Flow`, чтобы не перечитывать весь список. */
    @Query("SELECT COUNT(*) FROM notes")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun findById(id: Long): Note?

    /**
     * Поиск по подстроке в заголовке или тексте.
     *
     * `LIKE '%' || :query || '%'` — конкатенация внутри SQL, потому что `?`
     * нельзя использовать внутри строки: `?` заменяет **аргумент целиком**.
     * Наивное `LIKE '%?%'` ищет буквальный знак вопроса.
     */
    @Query(
        """
        SELECT * FROM notes
        WHERE title LIKE '%' || :query || '%' OR body LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
        """,
    )
    fun search(query: String): Flow<List<Note>>

    /** Возвращает сгенерированный `id` — поэтому `Long`, а не `Unit`. */
    @Insert
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query("DELETE FROM notes")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM notes")
    suspend fun count(): Int
}