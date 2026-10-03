package ru.devcustrom.androidlab.common

import android.app.Activity

/**
 * Одна лабораторная работа в списке главного экрана.
 *
 * [activityClass] — `null`, если лаба ещё не реализована. Это лучше, чем прятать
 * пункт: список всегда показывает все шесть работ и честно сообщает, какие из них
 * готовы. Когда появится Лаба 4, здесь достаточно заменить `null` на класс.
 *
 * @param number номер работы, для заголовка и сортировки
 * @param titleRes заголовок из `strings.xml`
 * @param summaryRes короткое описание, что внутри работы
 * @param activityClass экран лабы или `null`, если работа ещё не готова
 */
data class LabItem(
    val number: Int,
    val titleRes: Int,
    val summaryRes: Int,
    val activityClass: Class<out Activity>?,
) {
    /** Работа готова к запуску. */
    val isAvailable: Boolean get() = activityClass != null
}

/**
 * Список всех лабораторных — единственный источник правды о том, что готово.
 *
 * Вынесен в companion, а не собирается в `onCreate`, чтобы его можно было
 * проверить обычным unit-тестом без Android-контекста.
 */
object LabCatalog {

    val all: List<LabItem> = listOf(
        LabItem(
            number = 1,
            titleRes = ru.devcustrom.androidlab.R.string.lab1_title,
            summaryRes = ru.devcustrom.androidlab.R.string.lab1_summary,
            activityClass = ru.devcustrom.androidlab.lab1.Lab1Activity::class.java,
        ),
        LabItem(
            number = 2,
            titleRes = ru.devcustrom.androidlab.R.string.lab2_title,
            summaryRes = ru.devcustrom.androidlab.R.string.lab2_summary,
            activityClass = ru.devcustrom.androidlab.lab2.Lab2Activity::class.java,
        ),
        LabItem(
            number = 3,
            titleRes = ru.devcustrom.androidlab.R.string.lab3_title,
            summaryRes = ru.devcustrom.androidlab.R.string.lab3_summary,
            activityClass = ru.devcustrom.androidlab.lab3.AboutActivity::class.java,
        ),
        LabItem(
            number = 4,
            titleRes = ru.devcustrom.androidlab.R.string.lab4_title,
            summaryRes = ru.devcustrom.androidlab.R.string.lab4_summary,
            activityClass = null,
        ),
        LabItem(
            number = 5,
            titleRes = ru.devcustrom.androidlab.R.string.lab5_title,
            summaryRes = ru.devcustrom.androidlab.R.string.lab5_summary,
            activityClass = null,
        ),
        LabItem(
            number = 6,
            titleRes = ru.devcustrom.androidlab.R.string.lab6_title,
            summaryRes = ru.devcustrom.androidlab.R.string.lab6_summary,
            activityClass = null,
        ),
    )
}