package com.example.domain.usecase.bookmark

import com.example.domain.repository.DrugRepository

class RemoveBookmarkUseCase(
    private val repository: DrugRepository
) {
    suspend operator fun invoke(id: String) {
        repository.removeBookmark(id)
    }
}

