package ru.devcustrom.androidlab.data.model

import kotlinx.serialization.Serializable

/**
 * Единица величины из `data/units.json`.
 *
 * Имя поля [toMeters] историческое: в первой версии лабы конвертировали только длину,
 * и «сколько метров в единице» — буквальный смысл. Сейчас в файле три группы
 * (длина, масса, объём), поэтому точнее было бы `toBase`. Название оставлено прежним,
 * чтобы не расходиться с планом лабы; смысл такой же — сколько **базовых единиц своей
 * группы** содержится в одной этой единице.
 */
@Serializable
data class Unit(
    val id: String,
    val name: String,
    val short: String,
    val kind: String,
    val toMeters: Double,
) {
    val label: String get() = "$name ($short)"
}

/** Группа единиц, между которыми разрешена конвертация. */
enum class UnitKind(val id: String, val titleRes: String) {
    LENGTH("length", "kind_length"),
    MASS("mass", "kind_mass"),
    VOLUME("volume", "kind_volume"),
    ;

    companion object {
        fun from(id: String): UnitKind? = entries.firstOrNull { it.id == id }
    }
}

/** Обёртка над содержимым `units.json`. */
@Serializable
data class UnitsFile(
    val version: Int = 1,
    val units: List<Unit> = emptyList(),
)

/** Единицы одной группы, готовые к показу в `Spinner`. */
data class UnitGroup(
    val kind: UnitKind,
    val units: List<Unit>,
)