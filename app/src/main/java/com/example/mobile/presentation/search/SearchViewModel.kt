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

class SearchViewModel(
    private val searchDrugsUseCase: SearchDrugsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
    }
//rgrge
    fun onSearchClicked() {
        val query = _uiState.value.query
        searchJob?.cancel()
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
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    results = emptyList(),
                    isNoNetwork = error.isNetworkError()
                )
            }
        }
    }

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

