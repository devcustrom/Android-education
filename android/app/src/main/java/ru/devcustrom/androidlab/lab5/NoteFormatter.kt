package ru.devcustrom.androidlab.lab5

import android.text.format.DateUtils
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * Как подписать заметку: относительным временем или абсолютной датой.
 *
 * Вынесено отдельной чистой функцией, чтобы решение можно было проверить
 * обычным unit-тестом, а не только глазами на эмуляторе.
 */
enum class TimestampKind {
    /** Время не задано — подписывать нечем. */
    UNKNOWN,

    /** Меньше минуты назад — «только что», а не «0 мин. назад». */
    JUST_NOW,

    /** «5 минут назад», «вчера» — пока разница маленькая. */
    RELATIVE,

    /** Разница больше недели — «3 сент. 2026 г.». */
    ABSOLUTE,
}

/**
 * Правило выбора подписи.
 *
 * ```
 *  epochMillis <= 0        → UNKNOWN     («—»)
 *  меньше минуты назад    → JUST_NOW    («только что»)
 *  будущее время           → ABSOLUTE    (не «через 3 дня»!)
 *  меньше недели назад     → RELATIVE
 *  неделя и больше         → ABSOLUTE
 * ```
 *
 * Будущее время — не ошибка, а обычная ситуация: часы телефона выставили вперёд,
 * заметка сохранилась со старым `updatedAt`. `getRelativeTimeSpanString` в этом
 * случае честно напишет «через 3 дня», но для списка заметок это выглядит как
 * глюк.
 */
fun timestampKind(epochMillis: Long, now: Long): TimestampKind = when {
    epochMillis <= 0L -> TimestampKind.UNKNOWN
    epochMillis > now -> TimestampKind.ABSOLUTE
    now - epochMillis < DateUtils.MINUTE_IN_MILLIS -> TimestampKind.JUST_NOW
    now - epochMillis > DateUtils.WEEK_IN_MILLIS -> TimestampKind.ABSOLUTE
    else -> TimestampKind.RELATIVE
}

/**
 * Подпись для списка.
 *
 * `justNow` передаётся строкой, а не берётся из ресурсов внутри функции:
 * так её не нужно доставать из `Context`, и она остаётся тестируемой.
 */
fun formatTimestamp(
    epochMillis: Long,
    justNow: String,
    now: Long = System.currentTimeMillis(),
): String = when (timestampKind(epochMillis, now)) {
    TimestampKind.UNKNOWN -> "—"

    TimestampKind.JUST_NOW -> justNow

    TimestampKind.ABSOLUTE -> DateFormat
        .getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
        .format(Date(epochMillis))

    TimestampKind.RELATIVE -> DateUtils.getRelativeTimeSpanString(
        epochMillis,
        now,
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()
}