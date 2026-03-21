package com.example.domain.usecase.bookmark

import com.example.domain.model.Bookmark
import com.example.domain.repository.DrugRepository

/** Получение списка папок закладок. */
class GetBookmarksUseCase(
    private val repository: DrugRepository
) {
    suspend operator fun invoke(): List<Bookmark> {
        return repository.getBookmarks()
    }
}

