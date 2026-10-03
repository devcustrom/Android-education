package ru.devcustrom.androidlab.data.repository

import android.util.Log
import ru.devcustrom.androidlab.data.model.Cat
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

/**
 * Файловый кэш котов: `cats_cache.json` рядом с данными приложения.
 *
 * Вынесен из [CatRepository] не ради красоты, а ради тестов: класс работает с
 * обычным [File] и не знает про Android, поэтому проверяется обычным
 * unit-тестом на временном каталоге. `CatRepository` остаётся тем, что знает
 * про сеть.
 *
 * ```
 *  loadCats():  сеть → CatCache.write() → отдать список
 *  сеть упала → CatCache.read()  → отдать список или null
 *  Лаба 4    → CatCache.readText() / clear() / remove()
 * ```
 */
class CatCache(private val file: File) {

    /** Есть ли файл кэша. */
    fun exists(): Boolean = file.exists()

    /** Размер в байтах; `0`, если файла нет. */
    fun sizeBytes(): Long = if (file.exists()) file.length() else 0L

    /** Сам файл: нужен экрану Лабы 4 для экспорта и просмотра. */
    val sourceFile: File get() = file

    /** Абсолютный путь — чтобы показать пользователю в интерфейсе. */
    val path: String get() = file.absolutePath

    /** Сырое содержимое файла для «показать кэш». */
    fun readText(): String? = runCatching {
        if (file.exists()) file.readText() else null
    }.onFailure { Log.w(TAG, "Не смог прочитать кэш: ${it.message}") }.getOrNull()

    /** Записать котов в кэш. Ошибка не пробрасывается: кэш — не критичный путь. */
    fun write(cats: List<Cat>) {
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(cats))
            Log.i(TAG, "Кэш записан: ${cats.size} котов, ${file.length()} байт")
        }.onFailure { Log.w(TAG, "Не смог записать кэш: ${it.message}") }
    }

    fun read(): List<Cat>? = runCatching {
        if (!file.exists()) return@runCatching null
        json.decodeFromString<List<Cat>>(file.readText())
    }.onFailure { Log.w(TAG, "Кэш повреждён: ${it.message}") }.getOrNull()

    /** Удалить кэш целиком. */
    fun clear(): Boolean = runCatching {
        file.delete()
    }.onFailure { Log.w(TAG, "Не смог удалить кэш: ${it.message}") }.getOrDefault(false)

    /**
     * Убрать одного кота из кэша.
     *
     * Если после удаления не осталось ни одного кота, файл тоже удаляется:
     * пустой `[]` и отсутствие файла для приложения равнозначны, но «файла нет»
     * честнее показывается пользователю в интерфейсе.
     */
    fun remove(id: String): Boolean = runCatching {
        val cats = read() ?: return@runCatching false
        val rest = cats.filterNot { it.id == id }
        if (rest.size == cats.size) return@runCatching false
        if (rest.isEmpty()) file.delete() else write(rest)
        true
    }.onFailure { Log.w(TAG, "Не смог удалить кота из кэша: ${it.message}") }.getOrDefault(false)

    companion object {
        const val FILE_NAME = "cats_cache.json"

        internal val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        private const val TAG = "CatCache"

        /** Файл кэша внутри каталога данных приложения. */
        fun inDirectory(dataDir: File): CatCache = CatCache(File(dataDir, FILE_NAME))

        /** Кэш не создан — на экране честно пишем «нет кэша», а не «пусто». */
        fun requireNonEmpty(cats: List<Cat>?): List<Cat> =
            if (cats.isNullOrEmpty()) throw IOException("Кэш пуст или повреждён") else cats
    }
}