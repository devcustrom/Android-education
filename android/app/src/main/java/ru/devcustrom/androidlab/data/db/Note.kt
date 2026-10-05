package ru.devcustrom.androidlab.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Заметка — таблица `notes`.
 *
 * Разметка таблицы deliberately повторяет то, что уже есть в коде:
 * никаких «наследуемых» полей и преобразований. Три решения, которые стоит
 * увидеть сразу:
 *
 * * `autoGenerate = true` — `id` назначает сама база, поэтому в коде создания
 *   заметки можно оставить `id = 0L` и получить настоящий `Long` обратно из
 *   `@Insert`. Именно поэтому `id` — `Long`, а не `Int`: счётчик в SQLite
 *   длинный, и через несколько миллиардов записей `Int` переполнится.
 * * `createdAt` и `updatedAt` — **два разных** времени. Момент создания не
 *   меняется никогда, момент последнего изменения меняется при каждом
 *   сохранении. Одно поле «дата» не может ответить на оба вопроса.
 * * `Index` по `updatedAt` — список всегда сортируется по нему, а без индекса
 *   каждая выдача это full scan таблицы.
 */
@Entity(
    tableName = "notes",
    indices = [Index("updatedAt")],
)
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "body")
    val body: String = "",

    /** Момент создания, миллисекунды epoch. Не меняется при редактировании. */
    @ColumnInfo(name = "createdAt")
    val createdAt: Long,

    /** Момент последнего изменения. По нему сортируется список. */
    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long,
) {
    /** Пустая заметка — для создания, когда ещё нет никаких данных. */
    val isBlank: Boolean get() = title.isBlank() && body.isBlank()

    /**
     * Заголовок для списка: первая строка [title], без переводов строк.
     * Пустая строка означает «подставить заглушку из ресурсов» — сам текст
     * заглушки в модель не попадает, иначе в ней снова появятся строки
     * интерфейса, которые нельзя перевести.
     */
    val titleLine: String get() = title.takeFirstLine()

    /** Первая непустая строка [body]. Может совпадать с [titleLine] или быть пустой. */
    val previewLine: String get() = body.takeFirstLine()
}

/**
 * Первая непустая строка текста.
 *
 * Заметки пишут многострочно, а в списке нужна одна строка. `lines()`
 * для текста только из переводов строк даёт пустые строки, поэтому пустой
 * результат означает «показать нечего», а не «показать прочерк»: адаптер сам
 * решит, прятать ли превью.
 */
fun String.takeFirstLine(): String = lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()