package ru.devcustrom.androidlab.common

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.devcustrom.androidlab.R
import ru.devcustrom.androidlab.databinding.ItemLabBinding

/**
 * Адаптер списка лабораторных.
 *
 * Отдельный класс, а не анонимный адаптер внутри `MainActivity`: список стал
 * данными ([LabCatalog.all]), а не вёрсткой, и его теперь можно покрыть тестами.
 *
 * Долгое нажатие обрабатывается здесь, а не через `registerForContextMenu`:
 * у `RecyclerView` нет `getContextMenuInfo()`, поэтому позицию нажатой строки
 * знает только адаптер. Он и передаёт её дальше вместе с `View`-якорем.
 */
class LabAdapter(
    private val onClick: (LabItem) -> Unit,
    private val onLongClick: (LabItem, View) -> Unit,
) : ListAdapter<LabItem, LabAdapter.LabViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabViewHolder {
        val binding = ItemLabBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LabViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LabViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class LabViewHolder(
        private val binding: ItemLabBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LabItem) = with(binding) {
            val context = root.context

            numberText.text = context.getString(R.string.lab_number_format, item.number)
            titleText.setText(item.titleRes)
            summaryText.setText(item.summaryRes)

            // Недоступные лабы остаются в списке, но выглядят именно так, чтобы
            // было видно: пункт есть, работать пока не с чем.
            root.isEnabled = item.isAvailable
            root.isClickable = item.isAvailable
            root.alpha = if (item.isAvailable) 1f else 0.5f
            statusText.setText(
                if (item.isAvailable) R.string.lab_status_ready else R.string.lab_status_soon
            )

            root.setOnClickListener { if (item.isAvailable) onClick(item) }

            // `true` = «обработал»: иначе событие уйдёт дальше, и Android
            // попытается показать контекстное меню от самого RecyclerView.
            root.setOnLongClickListener { view ->
                onLongClick(item, view)
                true
            }
        }
    }

    private object Diff : DiffUtil.ItemCallback<LabItem>() {
        override fun areItemsTheSame(oldItem: LabItem, newItem: LabItem): Boolean =
            oldItem.number == newItem.number

        override fun areContentsTheSame(oldItem: LabItem, newItem: LabItem): Boolean =
            oldItem == newItem
    }
}