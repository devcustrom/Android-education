package ru.devcustrom.androidlab.lab1

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import ru.devcustrom.androidlab.R
import ru.devcustrom.androidlab.data.model.Unit
import ru.devcustrom.androidlab.data.model.UnitKind

/**
 * Адаптер для `Spinner` со списком единиц.
 *
 * Наследуемся от [ArrayAdapter] — это «старший брат ListAdapter»: умеет
 * сортировку, фильтр и `getItem()` без ручного `DiffUtil`. Для плоского
 * списка строк ничего умнее и не нужно.
 *
 * Разметку элемента переопределяем через [getView]: без этого `Spinner`
 * покажет `Unit(id=cm, name=Сантиметр, …)` — то есть `data class toString()`.
 */
class UnitAdapter(
    context: Context,
    units: List<Unit> = emptyList(),
) : ArrayAdapter<Unit>(context, R.layout.item_unit, units.toTypedArray()) {

    private val inflater = LayoutInflater.from(context)

    /** Заменяет содержимое списка целиком. */
    fun submit(newUnits: List<Unit>) {
        setNotifyOnChange(false)
        clear()
        addAll(newUnits)
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.item_unit, parent, false)
        view.findViewById<TextView>(R.id.unitTitle).text = getItem(position)?.label.orEmpty()
        return view
    }

    /**
     * Раскрывающийся список. Если его не переопределить, `ArrayAdapter`
     * соберёт элемент через `toString()` — и в выпадающем списке снова
     * окажется `Unit(id=cm, name=Сантиметр, …)`.
     */
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
        getView(position, convertView, parent)
}

/** Элемент верхнего `Spinner`: заголовок группы + её идентификатор. */
data class KindItem(val kind: UnitKind, val title: String)

/** Адаптер верхнего `Spinner` — выбор группы (длина / масса / объём). */
class KindAdapter(
    context: Context,
    items: List<KindItem> = emptyList(),
) : ArrayAdapter<KindItem>(context, R.layout.item_kind, items.toTypedArray()) {

    private val inflater = LayoutInflater.from(context)

    fun submit(newItems: List<KindItem>) {
        setNotifyOnChange(false)
        clear()
        addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.item_kind, parent, false)
        view.findViewById<TextView>(R.id.kindTitle).text = getItem(position)?.title.orEmpty()
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
        getView(position, convertView, parent)
}