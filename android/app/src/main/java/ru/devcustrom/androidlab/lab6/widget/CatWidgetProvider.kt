package ru.devcustrom.androidlab.lab6.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.Request
import ru.devcustrom.androidlab.R
import ru.devcustrom.androidlab.data.network.CataasApi
import ru.devcustrom.androidlab.data.network.HttpClientProvider
import ru.devcustrom.androidlab.lab6.Lab6Activity
import java.io.IOException

/**
 * Виджет рабочего стола «Котёнок дня»: картинка случайного кота с CATAAS.
 *
 * Три вещи, которые нужно знать про `AppWidgetProvider`:
 * - **`onUpdate` — не место для сети.** Он вызывается в главном потоке на широковещательном
 *   приёме, а система даёт всего ~10 секунд. Поэтому картинка грузится в корутине,
 *   а «висящий» процесс удерживается через [goAsync] + `finish()`.
 * - **Обновить интерфейс виджета напрямую нельзя** — только через [RemoteViews].
 *   Это «чертёж» для процесса лаунчера, а не живой `View`.
 * - **Периодичность задаётся в `widget_info.xml`** (`updatePeriodMillis`), минимум —
 *   30 минут. Чаще — только по кнопке, через `PendingIntent` с явной рассылкой.
 */
class CatWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        // Сначала отдаём «заглушку», чтобы виджет не был пустым, пока идёт сеть.
        updatePlaceholders(context, appWidgetManager, appWidgetIds)
        refreshCats(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, CatWidgetProvider::class.java))
            updatePlaceholders(context, manager, ids)
            refreshCats(context)
        }
    }

    // ------------------------------------------------------------------ обновление

    private fun refreshCats(context: Context) {
        // goAsync() продлевает жизнь приёмника, пока корутина не позовёт finish().
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {
            try {
                val bitmap = loadRandomCatImage()
                updateAll(context, bitmap, null)
            } catch (error: Exception) {
                updateAll(context, null, error.message ?: error.javaClass.simpleName)
            } finally {
                pendingResult.finish()
                scope.cancel()
            }
        }
    }

    private fun updatePlaceholders(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray,
    ) {
        val loading = context.getString(R.string.lab6_widget_empty)
        ids.forEach { id -> manager.updateAppWidget(id, buildViews(context, null, loading)) }
    }

    private fun updateAll(context: Context, bitmap: Bitmap?, status: String?) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, CatWidgetProvider::class.java))
        ids.forEach { id -> manager.updateAppWidget(id, buildViews(context, bitmap, status)) }
    }

    /**
     * [RemoteViews] — описание того, что лаунчер нарисует у себя. Обычные `findViewById`
     * и установка значений здесь не работают: есть только набор `set*`-методов.
     */
    private fun buildViews(context: Context, bitmap: Bitmap?, status: String?): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_cat)

        views.setTextViewText(
            R.id.widget_title,
            status ?: context.getString(R.string.lab6_widget_title),
        )

        if (bitmap != null) {
            views.setImageViewBitmap(R.id.widget_image, bitmap)
        } else {
            views.setImageViewResource(R.id.widget_image, R.drawable.cat_placeholder)
        }

        // Кнопка «Обновить кота» шлёт широковещание в этот же приёмник.
        views.setOnClickPendingIntent(R.id.widget_button, refreshIntent(context))

        // Тап по картинке открывает Лабу 6 — оттуда виджет легко найти и настроить.
        views.setOnClickPendingIntent(R.id.widget_image, openLabIntent(context))

        return views
    }

    private fun refreshIntent(context: Context): PendingIntent {
        val intent = Intent(context, CatWidgetProvider::class.java).setAction(ACTION_REFRESH)
        // FLAG_IMMUTABLE обязателен с API 31: система больше не разрешает
        // изменяемые PendingIntent без явного флага.
        return PendingIntent.getBroadcast(
            context,
            REQUEST_REFRESH,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun openLabIntent(context: Context): PendingIntent {
        val intent = Intent(context, Lab6Activity::class.java)
        return PendingIntent.getActivity(
            context,
            REQUEST_OPEN,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    // --------------------------------------------------------------------- сеть

    private suspend fun loadRandomCatImage(): Bitmap {
        val cat = CataasApi().getRandomCat()
        val bytes = download(cat.imageUrl)
        return decodeScaled(bytes, MAX_SIDE_PX)
            ?: throw IOException("Не удалось декодировать картинку кота")
    }

    private fun download(url: String): ByteArray {
        val request = Request.Builder().url(url).build()
        HttpClientProvider.shared.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            return response.body?.bytes() ?: throw IOException("Пустой ответ")
        }
    }

    /**
     * CATAAS отдаёт картинки «как есть» — это может быть несколько мегапикселей.
     * В виджет такая картинка не влезет, а в память процесса — запросто. Поэтому
     * считаем `inSampleSize` по границам, не декодируя полное изображение.
     */
    private fun decodeScaled(bytes: ByteArray, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

        var sample = 1
        while (
            bounds.outWidth / sample > maxSide ||
            bounds.outHeight / sample > maxSide
        ) {
            sample *= 2
        }

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    companion object {
        /** Внутреннее действие: «перезагрузить кота», а не системное обновление виджета. */
        const val ACTION_REFRESH = "ru.devcustrom.androidlab.action.WIDGET_REFRESH_KOT"

        private const val REQUEST_REFRESH = 100
        private const val REQUEST_OPEN = 101
        private const val MAX_SIDE_PX = 512
    }
}
