package ru.devcustrom.androidlab.lab2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import ru.devcustrom.androidlab.R
import ru.devcustrom.androidlab.data.model.Cat
import ru.devcustrom.androidlab.databinding.ItemCatBinding

/**
 * Адаптер галереи на [ListAdapter].
 *
 * Почему `ListAdapter`, а не `RecyclerView.Adapter`: при обновлении списка
 * ручной адаптер вызывает `notifyDataSetChanged()` и пересоздаёт **все** элементы.
 * `ListAdapter` сравнивает старый и новый список через [DiffUtil] и трогает
 * только реально изменившиеся строки — на 20 котах разницы не видно, на 2000
 * она превращается в заметное подтормаживание.
 *
 * Три вещи, без которых `DiffUtil` работает неправильно:
 *
 * 1. **`areItemsTheSame`** — это про *идентичность*, а не про содержимое.
 *    Сравнивать надо по `id`, а не по позиции: вставка кота в начало списка
 *    сдвинет все индексы, и всё перерисуется.
 * 2. **`areContentsTheSame`** — про содержимое: те же теги, тот же `id`.
 * 3. **`DiffUtil` работает в фоне**, а `onBindViewHolder` — в главном потоке.
 *    Сравнение 2000 котов не должно подтормаживать прокрутку.
 */
class CatAdapter : ListAdapter<Cat, CatAdapter.CatViewHolder>(DIFF) {

    /** Клик по карточке — наружу, в Activity. */
    var onCatClick: ((Cat) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CatViewHolder {
        val binding = ItemCatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CatViewHolder(
        private val binding: ItemCatBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(cat: Cat) {
            binding.catTitle.text = cat.title
            binding.catId.text = cat.id

            // Coil сам решает: кэш есть — берёт из кэша, нет — качает и кладёт.
            // Передавать `R.drawable` как fallback нужно обязательно: без него
            // при неудачной загрузке останется пустое место без всякой подсказки.
            binding.catImage.load(cat.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.cat_placeholder)
                error(R.drawable.cat_error)
                fallback(R.drawable.cat_error)
            }

            binding.root.setOnClickListener { onCatClick?.invoke(cat) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<Cat>() {

            override fun areItemsTheSame(oldItem: Cat, newItem: Cat): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Cat, newItem: Cat): Boolean =
                oldItem == newItem
        }
    }
}