package ru.devcustrom.androidlab.lab1

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import ru.devcustrom.androidlab.R
import ru.devcustrom.androidlab.data.model.Unit
import ru.devcustrom.androidlab.data.model.UnitKind
import ru.devcustrom.androidlab.data.network.UnitsApi
import ru.devcustrom.androidlab.databinding.ActivityLab1Binding
import ru.devcustrom.androidlab.ui.UnitsUiState
import ru.devcustrom.androidlab.ui.UnitsViewModel
import kotlinx.coroutines.launch

/**
 * Лабораторная 1: конвертер величин на `Spinner`.
 *
 * Устройство экрана:
 * - `UnitsViewModel` загружает справочник и отдаёт состояние через `StateFlow`;
 * - Activity только подписывается на поток `repeatOnLifecycle` и перерисовывает view;
 * - пересчёт результата — чистая функция [UnitConverter], её можно покрыть тестами.
 */
class Lab1Activity : AppCompatActivity() {

    private lateinit var binding: ActivityLab1Binding

    private val viewModel: UnitsViewModel by viewModels {
        UnitsViewModel.Factory(UnitsApi(applicationContext))
    }

    private var groups: List<UnitGroupView> = emptyList()
    private var selectedKind: UnitKind? = null

    /**
     * Выбранные единицы по `id`. Храним именно идентификаторы, а не позиции:
     * после поворота и после `submit()` список может прийти в другом порядке,
     * и сохранённый индекс указал бы не туда.
     */
    private var selectedFromId: String? = null
    private var selectedToId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLab1Binding.inflate(layoutInflater)
        setContentView(binding.root)

        selectedKind = savedInstanceState?.getString(STATE_KIND)?.let(UnitKind::from)
        selectedFromId = savedInstanceState?.getString(STATE_FROM)
        selectedToId = savedInstanceState?.getString(STATE_TO)

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.retryButton.setOnClickListener { viewModel.load() }
        binding.swapButton.setOnClickListener { swapSpinners() }

        binding.valueInput.doAfterTextChanged { recalculate() }

        binding.groupSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                bindGroup(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) { }
        }

        binding.fromSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                rememberSelections()
                recalculate()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) { }
        }

        binding.toSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                rememberSelections()
                recalculate()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) { }
        }

        observeState()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        selectedKind?.let { outState.putString(STATE_KIND, it.id) }
        selectedFromId?.let { outState.putString(STATE_FROM, it) }
        selectedToId?.let { outState.putString(STATE_TO, it) }
    }

    /**
     * `repeatOnLifecycle(STARTED)` — подписка живёт ровно пока экран видим,
     * иначе отменяется. На каждый новый запуск экрана подписка создаётся заново.
     */
    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when (state) {
                        is UnitsUiState.Loading -> showLoading()
                        is UnitsUiState.Ready -> showUnits(state)
                        is UnitsUiState.Error -> showError(state.message)
                    }
                }
            }
        }
    }

    private fun showLoading() = with(binding) {
        loadingBox.visibility = View.VISIBLE
        retryButton.visibility = View.GONE
        resultText.text = getString(R.string.lab1_result_placeholder)
        statusText.text = ""
    }

    private fun showError(message: String) = with(binding) {
        loadingBox.visibility = View.GONE
        retryButton.visibility = View.VISIBLE
        resultText.text = getString(R.string.lab1_result_placeholder)
        statusText.text = message
    }

    private fun showUnits(state: UnitsUiState.Ready) = with(binding) {
        loadingBox.visibility = View.GONE
        retryButton.visibility = View.GONE

        groups = state.groups.map { group ->
            UnitGroupView(
                kind = group.kind,
                title = getString(kindTitleRes(group.kind)),
                units = group.units,
            )
        }

        groupSpinner.adapter = KindAdapter(this@Lab1Activity, groups.map { KindItem(it.kind, it.title) })

        val restoreIndex = groups.indexOfFirst { it.kind == selectedKind }.takeIf { it >= 0 } ?: 0
        if (groups.isNotEmpty()) {
            groupSpinner.setSelection(restoreIndex, false)
            bindGroup(restoreIndex)
        }

        statusText.text = getString(
            if (state.source == UnitsApi.Source.CDN) R.string.source_cdn else R.string.source_assets
        )
    }

    /**
     * Наполняет оба `Spinner` единицами выбранной группы.
     *
     * Вызывается и при первом показе, и после поворота — поэтому вызов
     * идемпотентен: адаптеры пересоздаются всегда, а позиции восстанавливаются
     * по сохранённым `id`.
     */
    private fun bindGroup(position: Int) {
        val group = groups.getOrNull(position) ?: return

        selectedKind = group.kind

        binding.fromSpinner.adapter = UnitAdapter(this, group.units)
        binding.toSpinner.adapter = UnitAdapter(this, group.units)

        val fromIndex = group.units.indexOfFirst { it.id == selectedFromId }.takeIf { it >= 0 } ?: 0
        val toIndex = group.units.indexOfFirst { it.id == selectedToId }.takeIf { it >= 0 }
            ?: group.units.lastIndex

        selectedFromId = group.units[fromIndex].id
        selectedToId = group.units[toIndex].id

        binding.fromSpinner.setSelection(fromIndex, false)
        binding.toSpinner.setSelection(toIndex, false)

        recalculate()
    }

    private fun rememberSelections() {
        (binding.fromSpinner.selectedItem as? Unit)?.let { selectedFromId = it.id }
        (binding.toSpinner.selectedItem as? Unit)?.let { selectedToId = it.id }
    }

    private fun swapSpinners() = with(binding) {
        val from = fromSpinner.selectedItemPosition
        val to = toSpinner.selectedItemPosition
        fromSpinner.setSelection(to, false)
        toSpinner.setSelection(from, false)
        rememberSelections()
    }

    private fun recalculate() {
        val from = binding.fromSpinner.selectedItem as? Unit ?: return
        val to = binding.toSpinner.selectedItem as? Unit ?: return

        if (selectedKind == null) {
            binding.resultText.setText(R.string.lab1_empty_group)
            return
        }

        val raw = binding.valueInput.text?.toString()?.trim().orEmpty()
        if (raw.isEmpty()) {
            binding.inputLayout.error = null
            binding.resultText.setText(R.string.lab1_result_placeholder)
            return
        }

        val value = raw.replace(',', '.').toDoubleOrNull()
        if (value == null) {
            binding.inputLayout.error = getString(R.string.lab1_error_value)
            binding.resultText.setText(R.string.lab1_result_placeholder)
            return
        }
        binding.inputLayout.error = null

        val result = UnitConverter.convert(value, from, to)
        binding.resultText.text = getString(
            R.string.lab1_result_format,
            UnitConverter.format(value),
            from.short,
            UnitConverter.format(result),
            to.short,
        )
    }

    private fun kindTitleRes(kind: UnitKind): Int = when (kind) {
        UnitKind.LENGTH -> R.string.lab1_group_length
        UnitKind.MASS -> R.string.lab1_group_mass
        UnitKind.VOLUME -> R.string.lab1_group_volume
    }

    /** Группа единиц с уже переведённым заголовком — для адаптера `Spinner`. */
    private data class UnitGroupView(
        val kind: UnitKind,
        val title: String,
        val units: List<Unit>,
    )

    private companion object {
        const val STATE_KIND = "selected_kind"
        const val STATE_FROM = "selected_from"
        const val STATE_TO = "selected_to"
    }
}