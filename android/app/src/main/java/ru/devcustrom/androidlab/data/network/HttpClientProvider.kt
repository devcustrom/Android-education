package ru.devcustrom.androidlab.data.network

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Один общий [OkHttpClient] на всё приложение.
 *
 * Зачем он общий: каждый клиент держит свой пул соединений и свой кэш. Создавать
 * `OkHttpClient()` на каждый запрос — самая частая ошибка новичков: приложение
 * течёт по памяти и упирается в лимит открытых сокетов.
 */
object HttpClientProvider {

    val shared: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}