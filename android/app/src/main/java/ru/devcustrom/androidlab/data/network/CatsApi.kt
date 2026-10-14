package ru.devcustrom.androidlab.data.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.devcustrom.androidlab.data.model.Cat
import ru.devcustrom.androidlab.data.model.CommonsResponse
import ru.devcustrom.androidlab.data.model.toCat
import java.io.IOException

/**
 * Клиент галереи котов на базе **Wikimedia Commons**.
 *
 * Почему не CATAAS: его API и, что важнее, CDN картинок регулярно недоступны
 * (в том числе из РФ), а лаба должна работать предсказуемо. Commons отдаёт
 * и JSON, и сами изображения с `wikimedia.org`, доступность которых несравнимо
 * выше.
 *
 * Один запрос `action=query` с `generator=categorymembers` возвращает страницы
 * файлов из категории `Category:Cats`, а `prop=imageinfo` — их ссылки и метаданные.
 * Готовый список готовых котов (id, url, mime, timestamp) отдаёт [getCats],
 * случайного кота — [getRandomCat].
 */
class CatsApi(private val client: OkHttpClient = HttpClientProvider.shared) {

    /** Список котов из категории Commons. */
    suspend fun getCats(limit: Int = DEFAULT_LIMIT): List<Cat> = withContext(Dispatchers.IO) {
        val url = listUrl(limit).toString()
        Log.d(TAG, "GET $url")
        json.decodeFromString<CommonsResponse>(execute(url))
            .query?.pages.orEmpty()
            .map { it.toCat() }
            .filter { it.url.isNotBlank() }
    }

    /** Один случайный кот: берём пул из категории и выбираем случайного. */
    suspend fun getRandomCat(): Cat = withContext(Dispatchers.IO) {
        getCats(RANDOM_POOL).randomOrNull()
            ?: throw IOException("Commons не вернул ни одного кота")
    }

    /**
     * Собираем URL через [okhttp3.HttpUrl.Builder], а не склейкой строк:
     * `Category:Cats` и `url|mime|timestamp` содержат символы, которые обязаны
     * быть закодированы (`:` и `|`), иначе Commons ответит ошибкой.
     */
    private fun listUrl(limit: Int) = BASE_URL.toHttpUrl().newBuilder()
        .addQueryParameter("action", "query")
        .addQueryParameter("format", "json")
        .addQueryParameter("formatversion", "2")
        .addQueryParameter("generator", "categorymembers")
        .addQueryParameter("gcmtitle", CATEGORY)
        .addQueryParameter("gcmtype", "file")
        .addQueryParameter("gcmlimit", limit.toString())
        .addQueryParameter("prop", "imageinfo")
        .addQueryParameter("iiprop", "url|mime|timestamp")
        .addQueryParameter("iiurlwidth", Cat.THUMB_WIDTH.toString())
        .build()

    /**
     * Синхронный вызов OkHttp внутри `withContext(Dispatchers.IO)`.
     *
     * Так намеренно: в главном потоке сетевой вызов упадёт с
     * `NetworkOnMainThreadException`, а `suspend`-функция + `withContext` дают
     * ровно то же асинхронное поведение, но выглядят как обычный код.
     */
    private fun execute(url: String): String {
        val request = Request.Builder()
            .url(url)
            // Commons просит вежливый User-Agent; без него возможны отказы.
            .header("User-Agent", USER_AGENT)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} от $url")
            }
            return response.body?.string()
                ?: throw IOException("Пустой ответ от $url")
        }
    }

    private companion object {
        const val TAG = "CatsApi"
        const val DEFAULT_LIMIT = 20
        const val RANDOM_POOL = 20
        const val CATEGORY = "Category:Cats"
        const val BASE_URL = "https://commons.wikimedia.org/w/api.php"
        const val USER_AGENT = "AndroidLabs/1.0 (educational project)"

        /**
         * `ignoreUnknownKeys` обязателен: Commons добавляет поля, о которых модель
         * не знает. Без этого флага kotlinx.serialization бросит
         * `SerializationException` на первом же лишнем поле.
         */
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }
    }
}
