package ru.devcustrom.androidlab.data.network

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.devcustrom.androidlab.data.model.Unit
import ru.devcustrom.androidlab.data.model.UnitsFile
import java.io.IOException

/**
 * Загружает справочник единиц величин.
 *
 * Порядок источников:
 * 1. CDN — файл лежит в этом же репозитории и раздаётся через
 *    `cdn.jsdelivr.net/gh/<owner>/<repo>@main/data/units.json`.
 *    Так данные можно править в репозитории, не перевыпуская приложение.
 * 2. `assets/units.json` — та же копия файла, положенная в APK. Страховка:
 *    без интернета (или если jsDelivr ещё не закешировал коммит) приложение
 *    всё равно работает.
 */
class UnitsApi(
    private val context: Context,
    private val client: OkHttpClient = HttpClientProvider.shared,
) {

    /** Откуда в итоге взялись данные — показываем это внизу экрана. */
    enum class Source { CDN, ASSETS }

    data class Payload(val units: List<Unit>, val source: Source)

    suspend fun loadUnits(): Payload = withContext(Dispatchers.IO) {
        val fromCdn = runCatching { requestUnits() }.getOrNull()
        if (!fromCdn.isNullOrEmpty()) {
            Payload(fromCdn, Source.CDN)
        } else {
            Payload(readFromAssets(), Source.ASSETS)
        }
    }

    private fun requestUnits(): List<Unit> {
        val request = Request.Builder().url(UNITS_URL).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} по адресу $UNITS_URL")
            }
            val body = response.body?.string() ?: throw IOException("Пустой ответ сервера")
            return json.decodeFromString<UnitsFile>(body).units
        }
    }

    private fun readFromAssets(): List<Unit> =
        context.assets.open(ASSETS_FILE).bufferedReader().use { reader ->
            json.decodeFromString<UnitsFile>(reader.readText()).units
        }

    private companion object {
        const val UNITS_URL =
            "https://cdn.jsdelivr.net/gh/devcustrom/Android-education@main/data/units.json"
        const val ASSETS_FILE = "units.json"

        /**
         * `ignoreUnknownKeys` обязателен: в `units.json` есть поле `comment`,
         * о котором модель не знает. Без этого флага kotlinx.serialization бросает
         * `SerializationException` на любом лишнем поле — типичная грабля.
         */
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
}