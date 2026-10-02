package ru.devcustrom.androidlab.lab1

import ru.devcustrom.androidlab.data.model.Unit
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Чистая арифметика конвертера — без Android, без UI.
 *
 * Вынесено отдельно намеренно: так формулу можно проверить обычным
 * unit-тестом (`app/src/test/.../UnitConverterTest.kt`), не запуская эмулятор.
 */
object UnitConverter {

    /**
     * `value * from.toMeters / to.toMeters`.
     *
     * Идея: любая величина группы переводится в базовую единицу, а затем
     * из базовой — в целевую. Один шаг туда, один обратно.
     */
    fun convert(value: Double, from: Unit, to: Unit): Double {
        require(from.kind == to.kind) {
            "Нельзя конвертировать ${from.id} (${from.kind}) в ${to.id} (${to.kind})"
        }
        return value * from.toMeters / to.toMeters
    }

    /**
     * Убирает хвост из нулей: 1 вместо 1.000000, 0.0254 вместо 0.025400.
     * Округляем до 6 знаков — этого хватает для большинства конвертаций,
     * при этом дробная часть микроскопа не превращается в мусор.
     */
    fun format(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "—"
        return BigDecimal.valueOf(value)
            .setScale(6, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
    }
}