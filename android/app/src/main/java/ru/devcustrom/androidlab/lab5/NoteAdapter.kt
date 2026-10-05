package ru.devcustrom.androidlab.lab5

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.devcustrom.androidlab.R
import ru.devcustrom.androidlab.data.db.Note
import ru.devcustrom.androidlab.databinding.ItemNoteBinding

/**
 * Список заметок.
 *
 * `ListAdapter` вместо обычного `RecyclerView.Adapter` — не просто «удобно».
 * `notifyDataSetChanged()` пересоздаёт **все** видимые `ViewHolder`-ы, и на
 * списке из сотен заметок при прокрутке это заметно. `DiffUtil` сравнивает
 * старый и новый списки и перерисовывает только изменившиеся строки.
 *
 * Побочная выгода: `ListAdapter` не даст забыть про `notifyItemInserted` и
 * «забытый» вызов — компилятор и проверки `DiffUtil` сделают это заметным.
 */
class NoteAdapter(
    private val onClick: (Note) -> Unit,
) : ListAdapter<Note, NoteAdapter.Holder>(DIFF) {

    inner class Holder(private val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            // Один обработчик на весь список вместо обработчика в каждом Holder
            binding.root.setOnClickListener {
                currentList.getOrNull(bindingAdapterPosition)?.let(onClick)
            }
        }

        fun bind(note: Note) = with(binding) {
            val context = root.context

            noteTitle.text = note.titleLine.ifEmpty { context.getString(R.string.lab5_no_title) }

            // Превью прячется, если показывать нечего или если оно повторило бы
            // заголовок: заметка без заголовка начинается с текста, и без этой
            // проверки первая строка отрисовалась бы дважды подряд.
            val preview = note.previewLine
            val previewVisible = preview.isNotEmpty() && preview != note.titleLine
            notePreview.visibility = if (previewVisible) View.VISIBLE else View.GONE
            if (previewVisible) notePreview.text = preview

            noteMeta.text = context.getString(
                R.string.lab5_meta,
                formatTimestamp(note.updatedAt, context.getString(R.string.lab5_just_now)),
                note.id,
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false),
    )

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<Note>() {
            /** Идентичность — это `id`, а не содержимое: заголовок можно менять. */
            override fun areItemsTheSame(oldItem: Note, newItem: Note) = oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Note, newItem: Note) = oldItem == newItem
        }
    }
}