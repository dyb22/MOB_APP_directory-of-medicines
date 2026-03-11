package com.example.domain.usecase.history

import com.example.domain.repository.DrugRepository

class ClearSearchHistoryUseCase(
    private val repository: DrugRepository
) {
    suspend operator fun invoke() {
        repository.clearSearchHistory()
    }
}

