package ru.devcustrom.androidlab.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.devcustrom.androidlab.data.db.Note
import ru.devcustrom.androidlab.data.repository.NotesRepository

/**
 * `ViewModel` заметок: `Flow<List<Note>>` из DAO превращается в `StateFlow`
 * состояния экрана.
 *
 * ```
 *  DAO: Flow<List<Note>>            «горячий» поток базы: шлёт всем
 *       │
 *       ▼
 *  query.debounce(250)              пользователь печатает — ждём паузу
 *       │
 *       ▼
 *  flatMapLatest { repository.… }   новый символ ОТМЕНЯЕТ прошлый запрос
 *       │
 *       ▼
 *  stateIn(WhileSubscribed(5_000))  экран подписан — данные идут,
 *                                    все ушли — сборка встаёт
 * ```
 *
 * Три решения, ради которых эта лаба и нужна:
 *
 * 1. **`stateIn` вместо ручного `MutableStateFlow`.** Сам запускает сборку в
 *    `viewModelScope` и отдаёт `StateFlow`, который не теряет последнее
 *    значение при переподписке.
 * 2. **`WhileSubscribed(5_000)` вместо `Eagerly`.** После ухода всех
 *    подписчиков сборка встаёт через 5 секунд — ровно столько нужно системе,
 *    чтобы пересоздать Activity при повороте. `Eagerly` держал бы запросы
 *    живыми даже на экране «О программе», где заметок нет вовсе.
 * 3. **`flatMapLatest` для поиска.** Каждый символ переключает поток и
 *    **отменяет** предыдущий. Без него десять символов дают десять
 *    параллельных запросов, и ответы приходят не по порядке — быстрый
 *    «а» перезапишет медленный «абв» в списке.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class NotesViewModel(private val repository: NotesRepository) : ViewModel() {

    private val query = MutableStateFlow("")

    private val notes: Flow<List<Note>> =
        query.debounce(SEARCH_DEBOUNCE_MS)
            .flatMapLatest { text ->
                val q = text.trim()
                if (q.isEmpty()) repository.observeNotes() else repository.search(q)
            }
            .map { list -> list.filterNot { it.isBlank } }

    val uiState: StateFlow<NotesUiState> = notes
        .map { list ->
            when {
                list.isEmpty() -> NotesUiState.Empty
                else -> NotesUiState.Notes(list)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = NotesUiState.Loading,
        )

    /** Нужен экрану, чтобы показать плейсхолдер поиска. */
    val searchQuery: StateFlow<String> = query.asStateFlow()

    fun onQueryChanged(text: String) {
        if (query.value == text) return
        query.value = text
    }

    fun delete(note: Note) {
        viewModelScope.launch { repository.delete(note) }
    }

    /**
     * Возврат удалённого смахом. Именно [NotesRepository.restore], а не
     * [NotesRepository.save]: у удалённой заметки `id` остался, и `save` ушёл бы
     * в `@Update`, который для несуществующей строки молча ничего не делает.
     */
    fun restore(note: Note) {
        viewModelScope.launch { repository.restore(note, now = System.currentTimeMillis()) }
    }

    fun deleteAll() {
        viewModelScope.launch { repository.deleteAll() }
    }

    class Factory(private val repository: NotesRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NotesViewModel::class.java)) {
                "Неизвестный ViewModel: $modelClass"
            }
            return NotesViewModel(repository) as T
        }
    }

    companion object {
        /** Пауза перед поиском: быстрее — запрос на каждый символ. */
        const val SEARCH_DEBOUNCE_MS = 250L

        /**
         * Сколько ждать после ухода последнего подписчика, прежде чем остановить
         * сборку. Задержка нужна системе для пересоздания Activity при повороте:
         * без неё данные грузились бы заново при каждом повороте.
         */
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** Состояние экрана заметок. Пустой список — это состояние, а не «ничего». */
sealed interface NotesUiState {
    data object Loading : NotesUiState
    data object Empty : NotesUiState
    data class Notes(val list: List<Note>) : NotesUiState
}