package ru.devcustrom.androidlab.lab4

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.devcustrom.androidlab.data.model.Cat
import ru.devcustrom.androidlab.databinding.ItemCacheCatBinding

/**
 * Список котов из кэша с чекбоксами «выбрано».
 *
 * Отличие от [ru.devcustrom.androidlab.ui.CatAdapter] Лабы 2 — здесь нет
 * картинок и Coil: картинки из `filesDir` недоступны `ContentResolver`-у без
 * боли, а Лаба 4 про файлы, а не про загрузку изображений.
 *
 * Выделение хранится в [selected] рядом с данными и переживает поворот экрана:
 * см. `onSaveInstanceState` в `Lab4Activity`.
 */
class CacheCatAdapter(
    private val onToggle: (Cat) -> Unit,
    private val onLongPress: (Cat, android.view.View) -> Unit,
) : ListAdapter<Cat, CacheCatAdapter.Holder>(DIFF) {

    var selected: Set<String> = emptySet()
        private set

    fun toggleSelection(id: String) {
        selected = selected.toMutableSet().apply {
            if (!add(id)) remove(id)
        }
        notifyItemChanged(currentList.indexOfFirst { it.id == id })
    }

    fun restoreSelection(ids: Set<String>) {
        selected = ids
        notifyDataSetChanged()
    }

    fun clearSelection() {
        if (selected.isEmpty()) return
        selected = emptySet()
        notifyDataSetChanged()
    }

    inner class Holder(private val binding: ItemCacheCatBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                currentList.getOrNull(bindingAdapterPosition)?.let(onToggle)
            }
            binding.root.setOnLongClickListener { view ->
                currentList.getOrNull(bindingAdapterPosition)?.let { onLongPress(it, view) }
                true
            }
        }

        fun bind(cat: Cat) = with(binding) {
            catId.text = cat.id
            catTags.text = cat.tagLabel
            catUrl.text = cat.imageUrl
            checkBox.isChecked = cat.id in selected
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder = Holder(
        ItemCacheCatBinding.inflate(LayoutInflater.from(parent.context), parent, false),
    )

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<Cat>() {
            override fun areItemsTheSame(oldItem: Cat, newItem: Cat) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Cat, newItem: Cat) = oldItem == newItem
        }
    }
}
