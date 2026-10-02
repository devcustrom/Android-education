package ru.devcustrom.androidlab.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.devcustrom.androidlab.data.model.Cat
import ru.devcustrom.androidlab.data.repository.CatRepository

/** Что сейчас на экране галереи. */
sealed interface CatsUiState {

    /** Первая загрузка: скелет, крутилка, ничего не видно. */
    data object Loading : CatsUiState

    /**
     * Данные есть. [refreshing] отличает «обновляем список» от «грузим впервые»:
     * в первом случае список на экране остаётся, во втором — заменяется.
     */
    data class Success(
        val cats: List<Cat>,
        val origin: CatRepository.Origin,
        val refreshing: Boolean = false,
    ) : CatsUiState

    /** Данных нет и показать нечего. */
    data class Error(val message: String) : CatsUiState
}

/**
 * Загрузка котов.
 *
 * Работает и со всеми тегами, и с одним выбранным: [tags] — это фильтр,
 * который ViewModel применяет к уже загруженному списку. Благодаря этому
 * переключение тега не ходит в сеть и происходит мгновенно.
 */
class CatViewModel(private val repository: CatRepository) : ViewModel() {

    private val _state = MutableStateFlow<CatsUiState>(CatsUiState.Loading)
    val state: StateFlow<CatsUiState> = _state.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    /** Все скачанные коты — источник для фильтрации. */
    private var allCats: List<Cat> = emptyList()
    private var origin: CatRepository.Origin = CatRepository.Origin.NETWORK

    init {
        load()
    }

    /**
     * Загрузка сети. Повторный вызов во время загрузки игнорируется —
     * иначе быстрые тапы по «обновить» порождали бы десятки параллельных запросов.
     */
    fun load(force: Boolean = false) {
        val current = _state.value
        if (!force && current is CatsUiState.Success && current.refreshing) return

        _state.value = when {
            force && current is CatsUiState.Success ->
                current.copy(refreshing = true)

            else -> CatsUiState.Loading
        }

        viewModelScope.launch {
            _state.value = try {
                val result = repository.loadCats()
                allCats = result.cats
                origin = result.origin
                CatsUiState.Success(
                    cats = filterByTag(_selectedTag.value),
                    origin = result.origin,
                )
            } catch (e: Exception) {
                Log.e(TAG, "Не удалось загрузить котов", e)
                CatsUiState.Error(e.message ?: "Не удалось загрузить котов")
            }
        }
    }

    /** Переключение тега. Повторный выбор того же тега снимает фильтр. */
    fun selectTag(tag: String?) {
        _selectedTag.value = if (_selectedTag.value == tag) null else tag
        _state.value = if (allCats.isEmpty()) {
            _state.value
        } else {
            CatsUiState.Success(
                cats = filterByTag(_selectedTag.value),
                origin = origin,
            )
        }
    }

    /** Все уникальные теги, отсортированные — для `ChipGroup`. */
    fun allTags(): List<String> =
        allCats.flatMap { it.tags }.distinct().sorted()

    /** Счётчик котов под выбранным тегом — «пустые» чипы видно сразу. */
    fun countByTag(tag: String): Int = allCats.count { tag in it.tags }

    private fun filterByTag(tag: String?): List<Cat> =
        if (tag == null) allCats else allCats.filter { tag in it.tags }

    companion object {
        private const val TAG = "CatViewModel"
    }

    class Factory(private val repository: CatRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CatViewModel(repository) as T
    }
}