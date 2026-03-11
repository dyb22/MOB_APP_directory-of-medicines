package com.example.mobile.presentation.search

import com.example.domain.model.Drug

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<Drug> = emptyList(),
    val hasSearched: Boolean = false
)

