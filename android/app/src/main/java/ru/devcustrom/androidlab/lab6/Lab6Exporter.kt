package ru.devcustrom.androidlab.lab6

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream

/**
 * Сохранение рисунка в «Загрузки».
 *
 * Путь ровно тот же, что в `CacheExporter` (Лаба 4): `MediaStore.Downloads` с
 * `RELATIVE_PATH` на API 29+ и `externalFilesDir` ниже. Разница в источнике —
 * там был `File`, здесь `Bitmap`.
 *
 * ```
 *  Bitmap ──► compress(PNG) ──► OutputStream ──► Downloads/AndroidLab/
 *             100 (не сжимать)     MediaStore
 * ```
 *
 * `compress` пишет в поток целиком, поэтому `Bitmap` дополнительно оборачивать
 * в файл не нужно — на API 29+ это единственный способ обойтись без временного
 * файла в `cacheDir`.
 */
class Lab6Exporter(context: Context) {

    private val appContext = context.applicationContext

    sealed interface Result {
        data class Success(val location: String) : Result
        data class Failure(val reason: String) : Result
    }

    /**
     * @param fileName имя файла вместе с расширением, например `drawing.png`
     */
    fun export(bitmap: Bitmap, fileName: String = DEFAULT_NAME): Result = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            exportViaMediaStore(bitmap, fileName)
        } else {
            exportViaAppDirectory(bitmap, fileName)
        }
    }.getOrElse { Result.Failure(it.message ?: it.javaClass.simpleName) }

    private fun exportViaMediaStore(bitmap: Bitmap, fileName: String): Result {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, MIME_PNG)
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$EXPORT_DIR")
            // До завершения записи файл не должен появляться в галерее.
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val uri = appContext.contentResolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            values,
        ) ?: return Result.Failure("MediaStore не вернул Uri")

        val written = appContext.contentResolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, out)
        } ?: false

        if (!written) {
            // Запись не удалась — строку надо убрать, иначе в «Загрузках»
            // останется пустой файл на пару мегабайт.
            appContext.contentResolver.delete(uri, null, null)
            return Result.Failure("Bitmap не записался")
        }

        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        appContext.contentResolver.update(uri, values, null, null)

        // Не подставляем сюда локализованное «Загрузки»: Exporter не знает про
        // язык интерфейса, и в английской локали это выглядело бы как баг.
        // Показываем относительный путь, а слово «Загрузки/Downloads» при
        // необходимости добавляет строка ресурса на экране.
        return Result.Success("$EXPORT_DIR/$fileName")
    }

    private fun exportViaAppDirectory(bitmap: Bitmap, fileName: String): Result {
        @Suppress("DEPRECATION")
        val dir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: return Result.Failure("Нет внешнего каталога приложения")
        if (!dir.exists() && !dir.mkdirs()) return Result.Failure("Не создался каталог $dir")

        val target = File(dir, fileName)
        target.outputStream().use { out: OutputStream -> bitmap.writeToPng(out) }
        return Result.Success(target.absolutePath)
    }

    /** `compress` для PNG игнорирует quality, но API требует аргумент. */
    private fun Bitmap.writeToPng(out: OutputStream) {
        ByteArrayOutputStream().use { buffer ->
            compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, buffer)
            buffer.writeTo(out)
        }
    }

    companion object {
        private const val EXPORT_DIR = "AndroidLab"
        private const val MIME_PNG = "image/png"
        private const val PNG_QUALITY = 100
        private const val DEFAULT_NAME = "drawing.png"
    }
}