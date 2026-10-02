package ru.devcustrom.androidlab.data.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.devcustrom.androidlab.data.model.Cat
import ru.devcustrom.androidlab.data.model.RandomCatDto
import ru.devcustrom.androidlab.data.model.toCat
import java.io.IOException

/**
 * Клиент [CATAAS](https://cataas.com/) — бесплатный API с картинками котов.
 *
 * Два метода, две разные формы ответа:
 * - `getCats(limit)` — массив объектов, `/api/cats?limit=N`;
 * - `getRandomCat()` — один объект, `/cat?json=true`.
 */
class CataasApi(private val client: OkHttpClient = HttpClientProvider.shared) {

    /** Список последних котов. */
    suspend fun getCats(limit: Int = DEFAULT_LIMIT): List<Cat> = withContext(Dispatchers.IO) {
        val url = "$CATS_URL?limit=$limit"
        Log.d(TAG, "GET $url")
        json.decodeFromString<List<Cat>>(execute(url))
    }

    /** Один случайный кот. */
    suspend fun getRandomCat(): Cat = withContext(Dispatchers.IO) {
        Log.d(TAG, "GET $RANDOM_CAT_URL")
        json.decodeFromString<RandomCatDto>(execute(RANDOM_CAT_URL)).toCat()
    }

    /**
     * Синхронный вызов OkHttp внутри `withContext(Dispatchers.IO)`.
     *
     * OkHttp умеет и сам: `client.newCall(request).enqueue(…)`. Но для учебного
     * проекта честнее показать, что **сетевой вызов нельзя делать в главном
     * потоке**, — иначе приложение упадёт с `NetworkOnMainThreadException`.
     * `suspend`-функция + `withContext(Dispatchers.IO)` дают ровно то же
     * асинхронное поведение, но выглядят как обычный последовательный код.
     */
    private fun execute(url: String): String {
        val request = Request.Builder().url(url).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} от $url")
            }
            return response.body?.string()
                ?: throw IOException("Пустой ответ от $url")
        }
    }

    private companion object {
        const val TAG = "CataasApi"
        const val DEFAULT_LIMIT = 20
        const val CATS_URL = "https://cataas.com/api/cats"
        const val RANDOM_CAT_URL = "https://cataas.com/cat?json=true"

        /**
         * `ignoreUnknownKeys` обязателен: сервер добавляет поля, о которых модель
         * не знает (`_id`, `type`, `resizedUrl` и другие). Без этого флага
         * kotlinx.serialization бросит `SerializationException`.
         */
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }
    }
}