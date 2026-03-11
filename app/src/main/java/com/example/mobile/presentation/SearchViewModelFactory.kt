package com.example.mobile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.domain.usecase.drug.SearchDrugsUseCase
import com.example.mobile.presentation.search.SearchViewModel

class SearchViewModelFactory(
    private val searchDrugsUseCase: SearchDrugsUseCase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            return SearchViewModel(searchDrugsUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class $modelClass")
    }
}

