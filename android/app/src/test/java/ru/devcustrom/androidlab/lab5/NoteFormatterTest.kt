package ru.devcustrom.androidlab.lab5

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Проверяем то решение, которое нельзя увидеть глазами: какой именно вариант
 * подписи выбирается для граничных значений времени.
 *
 * Саму строку подписи (`formatTimestamp`) в unit-тесте проверять нельзя —
 * `DateUtils.getRelativeTimeSpanString` живёт в Android Framework, а в JVM
 * возвращает заглушку. Чистое решение ([timestampKind]) — можно.
 */
class NoteFormatterTest {

    private val now = 1_760_000_000_000L
    private val minute = 60_000L
    private val week = 7 * 24 * 60 * minute

    @Test
    fun `нулевое время не подписывается`() {
        assertEquals(TimestampKind.UNKNOWN, timestampKind(0L, now))
    }

    @Test
    fun `отрицательное время не подписывается`() {
        assertEquals(TimestampKind.UNKNOWN, timestampKind(-1L, now))
    }

    @Test
    fun `только что созданная заметка это только что а не ноль минут`() {
        assertEquals(TimestampKind.JUST_NOW, timestampKind(now, now))
        assertEquals(TimestampKind.JUST_NOW, timestampKind(now - 59_000L, now))
    }

    @Test
    fun `минута назад уже относительное время`() {
        assertEquals(TimestampKind.RELATIVE, timestampKind(now - minute, now))
    }

    @Test
    fun `ровно неделю назад еще относительное время`() {
        assertEquals(TimestampKind.RELATIVE, timestampKind(now - week, now))
    }

    @Test
    fun `больше недели назад уже абсолютная дата`() {
        assertEquals(TimestampKind.ABSOLUTE, timestampKind(now - week - minute, now))
    }

    /**
     * Ключевой грабли-сценарий: часы телефона перевели вперёд, заметка сохранена
     * со старым временем. Относительная подпись превратилась бы в «через 3 дня».
     */
    @Test
    fun `будущее время не превращается в через сколько-то дней`() {
        assertEquals(TimestampKind.ABSOLUTE, timestampKind(now + 3 * 24 * 60 * minute, now))
    }
}