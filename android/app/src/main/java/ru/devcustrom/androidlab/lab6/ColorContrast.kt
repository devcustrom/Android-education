package ru.devcustrom.androidlab.lab6

import android.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Подбор контрастного цвета текста под заливку.
 *
 * Нужно для палитры Лабы 6: белый текст на жёлтом читается плохо, чёрный — отлично,
 * и наоборот. Android не умеет «перекрасить текст под фон» автоматически, поэтому
 * контраст считается вручную.
 *
 * Формула — относительная яркость по WCAG 2.1:
 * ```
 *  C = 0.2126 R + 0.7152 G + 0.0722 B      (после гамма-развёртки)
 *  contrast = (max + 0.05) / (min + 0.05)
 * ```
 *
 * Все промежуточные вычисления ведутся в `Double`, а в `Float` переводится только
 * результат: если округлять каналы заранее, граничные значения вроде «чёрный на
 * белом ровно 21.0» уезжают на 20.99 и валят тест.
 *
 * Функция чистая и без Android UI, поэтому её можно проверить обычным тестом.
 */
object ColorContrast {

    /**
     * Возвращает белый или чёрный — тот, что контрастнее к [background].
     */
    fun forBackground(background: Int): Int {
        val contrastWithWhite = contrast(background, Color.WHITE)
        val contrastWithBlack = contrast(background, Color.BLACK)
        return if (contrastWithBlack >= contrastWithWhite) Color.BLACK else Color.WHITE
    }

    /**
     * Отношение контраста двух цветов: 1.0 — одинаковые, 21.0 — белый на чёрном.
     * Значение ниже 4.5 WCAG считает недостаточным для основного текста.
     */
    fun contrast(first: Int, second: Int): Float {
        val a = luminance(first).toDouble()
        val b = luminance(second).toDouble()
        val result = (max(a, b) + 0.05) / (min(a, b) + 0.05)
        return result.toFloat()
    }

    /**
     * Относительная яркость: каждый канал приводится из 0..255 в 0..1,
     * для значений меньше 0.03928 применяется линейная ветка, дальше — степенная.
     * Без этой поправки тёмные цвета считаются «слишком светлыми».
     */
    fun luminance(color: Int): Float {
        val opaque = (color.toLong() and 0xFFFFFFFFL).toInt()

        fun channel(raw: Int): Double {
            val value = raw.toDouble() / 255.0
            return if (value <= 0.03928) {
                value / 12.92
            } else {
                ((value + 0.055) / 1.055).pow(2.4)
            }
        }

        val r = channel((opaque shr 16) and 0xFF)
        val g = channel((opaque shr 8) and 0xFF)
        val b = channel(opaque and 0xFF)
        val lum = 0.2126 * r + 0.7152 * g + 0.0722 * b
        return lum.toFloat()
    }
}
