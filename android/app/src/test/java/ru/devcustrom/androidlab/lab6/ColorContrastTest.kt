package ru.devcustrom.androidlab.lab6

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Проверяем выбор контрастного цвета текста под заливку.
 *
 * Логика чистая, поэтому без моков Android Framework всё работает.
 */
class ColorContrastTest {

    @Test
    fun `на белом фоне чёрный лучше белого`() {
        assertEquals(Color.BLACK, ColorContrast.forBackground(Color.WHITE))
    }

    @Test
    fun `на чёрном фоне белый лучше чёрного`() {
        assertEquals(Color.WHITE, ColorContrast.forBackground(Color.BLACK))
    }

    @Test
    fun `контраст белого на белом минимальный`() {
        assertEquals(1f, ColorContrast.contrast(Color.WHITE, Color.WHITE), 0.0001f)
    }

    @Test
    fun `контраст чёрного на белом близок к 21`() {
        val c = ColorContrast.contrast(Color.BLACK, Color.WHITE)
        assertTrue(c > 20f && c <= 21f)
    }

    @Test
    fun `красный фон даёт контрастный текст`() {
        val color = 0xFFE53935.toInt()
        val text = ColorContrast.forBackground(color)
        assertEquals(Color.BLACK, text)
    }

    @Test
    fun `тёмно-синий даёт белый текст`() {
        val text = ColorContrast.forBackground(Color.parseColor("#0D47A1"))
        assertEquals(Color.WHITE, text)
    }
}