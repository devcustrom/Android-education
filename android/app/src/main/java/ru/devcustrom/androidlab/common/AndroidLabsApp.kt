package ru.devcustrom.androidlab.common

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import ru.devcustrom.androidlab.data.network.HttpClientProvider

/**
 * Точка входа приложения.
 *
 * Смысл класса один: отдать Coil **наш** `OkHttpClient`. По умолчанию Coil
 * создаёт собственный клиент со стандартным `User-Agent: okhttp/…`, а Wikimedia
 * на такой UA отвечает `403` — картинки котиков не загружались. Общий клиент
 * решает это одним местом, а не правкой каждого экрана.
 */
class AndroidLabsApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient { HttpClientProvider.shared }
        .crossfade(true)
        .build()
}
