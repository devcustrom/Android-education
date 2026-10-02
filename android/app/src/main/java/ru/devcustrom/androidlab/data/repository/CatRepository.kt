package ru.devcustrom.androidlab.data.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import ru.devcustrom.androidlab.data.model.Cat
import ru.devcustrom.androidlab.data.network.CataasApi
import java.io.File
import java.io.IOException

/**
 * Где взять котов: сначала сеть, при неудаче — кэш.
 *
 * Схема сознательно простая, без Room и без библиотек:
 *
 * ```
 *  сеть доступна?  ──да──►  GET cataas.com  ──ошибка?──►  кэш
 *        │                                                    │
 *        └──нет──►  кэш ◄─────────────  кэша нет? ──►  ошибка на экран
 * ```
 *
 * Кэш — обычный файл `cats_cache.json` в `filesDir`. Именно `filesDir`,
 * а не внешнее хранилище: доступ не нужно запрашивать разрешение, а после
 * удаления приложения кэш исчезает вместе с ним — и это правильное поведение
 * для временных данных.
 */
class CatRepository(
    private val context: Context,
    private val api: CataasApi = CataasApi(),
) {

    /** Что получилось в итоге: сеть, кэш или ничего. */
    enum class Origin { NETWORK, CACHE }

    data class LoadResult(val cats: List<Cat>, val origin: Origin)

    suspend fun loadCats(limit: Int = LIMIT): LoadResult = withContext(Dispatchers.IO) {
        val fromNetwork = runCatching { api.getCats(limit) }
            .onFailure { Log.w(TAG, "Сеть не ответила, пробуем кэш: ${it.javaClass.simpleName}: ${it.message}") }
            .getOrNull()

        if (!fromNetwork.isNullOrEmpty()) {
            saveToCache(fromNetwork)
            return@withContext LoadResult(fromNetwork, Origin.NETWORK)
        }

        val fromCache = readFromCache()
        if (!fromCache.isNullOrEmpty()) {
            Log.i(TAG, "Показываем кэш (${fromCache.size} шт.)")
            LoadResult(fromCache, Origin.CACHE)
        } else {
            throw IOException("Нет ни сети, ни кэша")
        }
    }

    /** Случайный кот: кэшировать его смысла нет, поэтому только сеть. */
    suspend fun randomCat(): Cat = api.getRandomCat()

    private fun cacheFile(): File = File(context.filesDir, CACHE_FILE)

    private fun saveToCache(cats: List<Cat>) {
        runCatching {
            cacheFile().writeText(json.encodeToString(cats))
            Log.i(TAG, "Кэш записан: ${cats.size} котов")
        }.onFailure { Log.w(TAG, "Не смог записать кэш: ${it.message}") }
    }

    private fun readFromCache(): List<Cat>? = runCatching {
        val file = cacheFile()
        if (!file.exists()) return@runCatching null
        json.decodeFromString<List<Cat>>(file.readText())
    }.getOrNull()

    private companion object {
        const val TAG = "CatRepository"
        const val CACHE_FILE = "cats_cache.json"
        const val LIMIT = 20

        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}