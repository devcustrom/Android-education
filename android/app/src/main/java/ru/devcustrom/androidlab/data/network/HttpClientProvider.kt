package ru.devcustrom.androidlab.data.network

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Один общий [OkHttpClient] на всё приложение.
 *
 * Зачем он общий: каждый клиент держит свой пул соединений и свой кэш. Создавать
 * `OkHttpClient()` на каждый запрос — самая частая ошибка новичков: приложение
 * течёт по памяти и упирается в лимит открытых сокетов.
 *
 * Этот же клиент отдаётся Coil (см. `AndroidLabsApp`), поэтому заголовки ниже
 * автоматически уезжают и при загрузке картинок.
 */
object HttpClientProvider {

    /**
     * Wikimedia отвечает `403` на стандартный `User-Agent: okhttp/…` — именно
     * поэтому у Coil «не грузились» картинки. Честный UA с названием проекта
     * решает проблему и заодно вежлив по отношению к открытым API.
     */
    const val USER_AGENT = "AndroidLabs/1.0 (educational project)"

    val shared: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}