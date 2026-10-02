package ru.devcustrom.androidlab.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.devcustrom.androidlab.data.model.UnitGroup
import ru.devcustrom.androidlab.data.model.UnitKind
import ru.devcustrom.androidlab.data.network.UnitsApi

/** Что сейчас происходит со справочником единиц. */
sealed interface UnitsUiState {
    data object Loading : UnitsUiState
    data class Ready(val groups: List<UnitGroup>, val source: UnitsApi.Source) : UnitsUiState
    data class Error(val message: String) : UnitsUiState
}

/**
 * Хранит справочник единиц и отдаёт его экрану через [StateFlow].
 *
 * ViewModel не знает ни про `Spinner`, ни про `TextView` — только про данные.
 * Экран подписан на поток и перерисовывается сам.
 */
class UnitsViewModel(private val api: UnitsApi) : ViewModel() {

    private val _state = MutableStateFlow<UnitsUiState>(UnitsUiState.Loading)
    val state: StateFlow<UnitsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UnitsUiState.Loading
        viewModelScope.launch {
            _state.value = try {
                val payload = api.loadUnits()
                val groups = UnitKind.entries.mapNotNull { kind ->
                    val group = payload.units.filter { it.kind == kind.id }
                    if (group.isEmpty()) null else UnitGroup(kind, group)
                }
                if (groups.isEmpty()) {
                    UnitsUiState.Error("Справочник пуст — проверьте data/units.json")
                } else {
                    UnitsUiState.Ready(groups, payload.source)
                }
            } catch (e: Exception) {
                UnitsUiState.Error(e.message ?: "Не удалось загрузить единицы")
            }
        }
    }

    class Factory(private val api: UnitsApi) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            UnitsViewModel(api) as T
    }
}