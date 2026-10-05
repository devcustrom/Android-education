package ru.devcustrom.androidlab.lab5

import ru.devcustrom.androidlab.data.db.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * У [Note] есть логика, которую видно в списке: пустая ли заметка и какая
 * строка идёт в заголовок, а какая — в превью. Room её не проверяет, а
 * пользователь видит — значит, тест нужен.
 */
class NoteTest {

    private fun note(title: String = "", body: String = "") =
        Note(title = title, body = body, createdAt = 0L, updatedAt = 0L)

    @Test
    fun `без текста заметка пустая`() {
        assertTrue(note().isBlank)
        assertTrue(note(title = "   ", body = "\n\t ").isBlank)
    }

    @Test
    fun `заметка с текстом не пустая`() {
        assertFalse(note(title = "Корм").isBlank)
        assertFalse(note(body = "Корм").isBlank)
    }

    @Test
    fun `заголовок обрезается до первой непустой строки`() {
        assertEquals("Первая", note(title = "\n\nПервая\nвторая").titleLine)
    }

    @Test
    fun `пустой заголовок даёт пустую строку а не заглушку`() {
        assertEquals("", note(title = "  ", body = "текст").titleLine)
    }

    @Test
    fun `превью это первая непустая строка тела`() {
        assertEquals("Вторая строка", note(body = "\n\nВторая строка\nтретья").previewLine)
    }

    @Test
    fun `превью пустое если тело состоит из одних переводов строк`() {
        assertEquals("", note(body = "\n \n").previewLine)
    }

    /**
     * Заметка без заголовка: `titleLine` пустой, весь текст уезжает в превью.
     * Именно поэтому адаптер подставляет в заголовок ресурс, а превью прячет —
     * иначе строка отрисовалась бы дважды подряд.
     */
    @Test
    fun `без заголовка весь текст уезжает в превью`() {
        val subject = note(title = "", body = "Корм и вода")
        assertEquals("", subject.titleLine)
        assertEquals("Корм и вода", subject.previewLine)
    }
}