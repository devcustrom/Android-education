package ru.devcustrom.androidlab.data.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.devcustrom.androidlab.data.model.Cat
import ru.devcustrom.androidlab.data.network.CataasApi
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
 *
 * Файловые операции вынесены в [CatCache], чтобы их можно было тестировать
 * без Android. Экран Лабы 4 работает с кэшем напрямую, минуя сеть.
 */
class CatRepository(
    private val context: Context,
    private val api: CataasApi = CataasApi(),
) {

    /** Что получилось в итоге: сеть, кэш или ничего. */
    enum class Origin { NETWORK, CACHE }

    data class LoadResult(val cats: List<Cat>, val origin: Origin)

    val cache: CatCache = CatCache.inDirectory(context.filesDir)

    suspend fun loadCats(limit: Int = LIMIT): LoadResult = withContext(Dispatchers.IO) {
        val fromNetwork = runCatching { api.getCats(limit) }
            .onFailure { Log.w(TAG, "Сеть не ответила, пробуем кэш: ${it.javaClass.simpleName}: ${it.message}") }
            .getOrNull()

        if (!fromNetwork.isNullOrEmpty()) {
            cache.write(fromNetwork)
            return@withContext LoadResult(fromNetwork, Origin.NETWORK)
        }

        val fromCache = cache.read()
        if (!fromCache.isNullOrEmpty()) {
            Log.i(TAG, "Показываем кэш (${fromCache.size} шт.)")
            LoadResult(fromCache, Origin.CACHE)
        } else {
            throw IOException("Нет ни сети, ни кэша")
        }
    }

    /** Случайный кот: кэшировать его смысла нет, поэтому только сеть. */
    suspend fun randomCat(): Cat = api.getRandomCat()

    private companion object {
        const val TAG = "CatRepository"
        const val LIMIT = 20
    }
}