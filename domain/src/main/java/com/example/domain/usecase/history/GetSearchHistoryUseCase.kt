package com.example.domain.usecase.history

import com.example.domain.model.SearchHistoryItem
import com.example.domain.repository.DrugRepository

class GetSearchHistoryUseCase(
    private val repository: DrugRepository
) {
    suspend operator fun invoke(): List<SearchHistoryItem> {
        return repository.getSearchHistory()
    }
}

