package ru.devcustrom.androidlab.lab6

import android.graphics.Color

/**
 * Палитра и толщины для [DrawingView].
 *
 * Вынесено в объект, потому что это единственное место в приложении, где
 * «какого цвета линия» — это данные, а не ресурс: цвета рисуются в `Canvas`
 * напрямую, минуя тему оформления.
 */
object BrushPalette {

    /**
     * Палитра подобрана так, чтобы все цвета были различимы на белом холсте
     * и не совпадали с системными состояниями элементов интерфейса.
     */
    val COLORS = listOf(
        Color.BLACK,
        Color.parseColor("#E53935"), // красный
        Color.parseColor("#1E88E5"), // синий
        Color.parseColor("#43A047"), // зелёный
        Color.parseColor("#FB8C00"), // оранжевый
        Color.parseColor("#8E24AA"), // фиолетовый
    )

    /**
     * Толщины в dp. `MIN` и `MAX` те же, что в `DrawingView`: список не должен
     * предлагать значения, которые view потом срежет.
     */
    val WIDTHS = listOf(2f, 6f, 12f, 20f)

    val DEFAULT_COLOR = COLORS.first()
    val DEFAULT_WIDTH = WIDTHS[1]
}