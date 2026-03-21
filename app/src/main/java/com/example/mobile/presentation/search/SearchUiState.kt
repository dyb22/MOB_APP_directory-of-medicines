package com.example.mobile.presentation.search

import com.example.domain.model.Drug

/** Состояние UI экрана поиска. Используется SearchViewModel и SearchFragment. */
data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<Drug> = emptyList(),
    /** Был ли уже выполнен хотя бы один поиск (для отображения «ничего не найдено») */
    val hasSearched: Boolean = false,
    val isNoNetwork: Boolean = false
)

