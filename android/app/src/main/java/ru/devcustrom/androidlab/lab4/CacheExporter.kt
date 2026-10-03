package ru.devcustrom.androidlab.lab4

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Экспорт `cats_cache.json` в «Загрузки».
 *
 * Два принципиально разных пути — и это главная мысль Лабы 4 про хранилища:
 *
 * ```
 *  API 29+                    MediaStore.Downloads + RELATIVE_PATH
 *     │                       разрешение НЕ нужно, файл виден пользователю
 *     │
 *  API 24..28                 getExternalFilesDir(DIRECTORY_DOWNLOADS)
 *     │                       тоже без разрешения, но лежит в «каталоге
 *     │                       приложения»: удалишь приложение — удалится файл
 * ```
 *
 * `WRITE_EXTERNAL_STORAGE` здесь не запрашивается намеренно. Он deprecated с
 * API 29 и всё равно не даёт писать в чужие каталоги на API 30+.
 *
 * [FileProvider] нужен для старого пути: на API 24+ нельзя отдать наружу
 * `Uri.fromFile(...)` — система выбросит `FileUriExposedException`.
 */
object CacheExporter {

    private const val TAG = "CacheExporter"
    private const val EXPORT_DIR = "AndroidLab"

    sealed interface Result {
        data class Success(val uri: Uri, val visibleToUser: Boolean, val location: String) : Result
        data class Failure(val message: String) : Result
    }

    suspend fun export(context: Context, source: File, fileName: String): Result =
        withContext(Dispatchers.IO) {
            if (!source.exists()) return@withContext Result.Failure("Нет файла ${source.name}")
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    exportViaMediaStore(context, source, fileName)
                } else {
                    exportViaAppDirectory(context, source, fileName)
                }
            }.getOrElse { Result.Failure(it.message ?: it.javaClass.simpleName) }
        }

    private fun exportViaMediaStore(context: Context, source: File, fileName: String): Result {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, MIME_JSON)
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$EXPORT_DIR")
            // Файл не должен появиться у пользователя, пока не дописан
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val uri = resolver.insert(collection, values)
            ?: return Result.Failure("MediaStore не вернул Uri")

        resolver.openOutputStream(uri).use { out ->
            if (out == null) return Result.Failure("Не открылся поток для записи")
            source.inputStream().use { input -> input.copyTo(out) }
        }

        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)

        Log.i(TAG, "Экспорт в «Загрузки»: $uri (${source.length()} байт)")
        return Result.Success(uri, visibleToUser = true, location = "Загрузки/$EXPORT_DIR/$fileName")
    }

    private fun exportViaAppDirectory(context: Context, source: File, fileName: String): Result {
        @Suppress("DEPRECATION")
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: return Result.Failure("Нет внешнего каталога приложения")
        if (!dir.exists() && !dir.mkdirs()) return Result.Failure("Не создался каталог $dir")

        val target = File(dir, fileName)
        source.copyTo(target, overwrite = true)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", target)
        Log.i(TAG, "Экспорт в каталог приложения: $target")
        return Result.Success(
            uri = uri,
            visibleToUser = false,
            location = "${target.absolutePath} (видно только приложению)",
        )
    }

    private const val MIME_JSON = "application/json"
}