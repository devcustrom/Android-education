package ru.devcustrom.androidlab.lab1

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import ru.devcustrom.androidlab.data.model.Unit

/**
 * Проверяем формулу без эмулятора: `./gradlew test` — и всё зелёное.
 *
 * `Unit` намеренно собирается руками: тесты не должны зависеть от сети или от
 * файла `data/units.json`, который в реальном приложении приходит с CDN.
 */
class UnitConverterTest {

    private val centimeter = Unit("cm", "Сантиметр", "см", "length", 0.01)
    private val millimeter = Unit("mm", "Миллиметр", "мм", "length", 0.001)
    private val meter = Unit("m", "Метр", "м", "length", 1.0)
    private val kilometer = Unit("km", "Километр", "км", "length", 1000.0)
    private val kilogram = Unit("kg", "Килограмм", "кг", "mass", 1.0)
    private val gram = Unit("g", "Грамм", "г", "mass", 0.001)

    @Test
    fun `2 сантиметра это 20 миллиметров`() {
        assertEquals(20.0, UnitConverter.convert(2.0, centimeter, millimeter), 1e-9)
    }

    @Test
    fun `100 сантиметров это 1 метр`() {
        assertEquals(1.0, UnitConverter.convert(100.0, centimeter, meter), 1e-9)
    }

    @Test
    fun `1 километр это 1000 метров`() {
        assertEquals(1000.0, UnitConverter.convert(1.0, kilometer, meter), 1e-9)
    }

    @Test
    fun `5 километров это 500000 сантиметров`() {
        assertEquals(500_000.0, UnitConverter.convert(5.0, kilometer, centimeter), 1e-6)
    }

    @Test
    fun `0 градусов не ломает арифметику`() {
        assertEquals(0.0, UnitConverter.convert(0.0, centimeter, kilometer), 1e-9)
    }

    @Test
    fun `отрицательное значение сохраняет знак`() {
        assertEquals(-500.0, UnitConverter.convert(-5.0, meter, centimeter), 1e-9)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `конвертировать длину в массу нельзя`() {
        UnitConverter.convert(1.0, meter, kilogram)
    }

    @Test
    fun `смешивание групп бросает исключение`() {
        assertThrows(IllegalArgumentException::class.java) {
            UnitConverter.convert(1.0, centimeter, kilogram)
        }
    }

    @Test
    fun `2_5 килограмма это 2500 граммов`() {
        assertEquals(2500.0, UnitConverter.convert(2.5, kilogram, gram), 1e-9)
    }

    @Test
    fun `формат убирает хвостовые нули`() {
        assertEquals("1", UnitConverter.format(1.000000))
        assertEquals("0.0254", UnitConverter.format(0.0254))
        assertEquals("1500", UnitConverter.format(1500.0))
    }

    @Test
    fun `формат округляет до шести знаков`() {
        assertEquals("0.333333", UnitConverter.format(1.0 / 3.0))
    }
}