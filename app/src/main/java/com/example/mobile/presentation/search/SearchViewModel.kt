package com.example.mobile.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.usecase.drug.SearchDrugsUseCase
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel экрана поиска. Управляет состоянием: запрос, загрузка, результаты, сетевая ошибка.
 * Отменяет предыдущий поиск при новом запросе. Сетевые исключения преобразует в isNoNetwork.
 */
class SearchViewModel(
    private val searchDrugsUseCase: SearchDrugsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    /** Job текущего поиска, для отмены при повторном нажатии */
    private var searchJob: Job? = null

    /** Обновление текста запроса в состоянии (без запуска поиска) */
    fun onQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
    }

    /** Запуск поиска. Отменяет предыдущий, устанавливает isLoading. */
    fun onSearchClicked() {
        val query = _uiState.value.query
        searchJob?.cancel() // отмена предыдущего поиска
        searchJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                hasSearched = true,
                isNoNetwork = false
            )
            runCatching {
                searchDrugsUseCase(query)
            }.onSuccess { drugs ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    results = drugs,
                    isNoNetwork = false
                )
            }.onFailure { error ->
                // isNoNetwork по цепочке cause
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    results = emptyList(),
                    isNoNetwork = error.isNetworkError()
                )
            }
        }
    }

    /** Проверка: это сетевая ошибка (UnknownHost, Connect, SocketTimeout или cause) */
    private fun Throwable.isNetworkError(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            if (current is UnknownHostException ||
                current is ConnectException ||
                current is SocketTimeoutException
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }
}

