package com.example.domain.usecase.bookmark

import com.example.domain.model.Bookmark
import com.example.domain.repository.DrugRepository

/** Создание новой папки закладок. */
class AddBookmarkUseCase(
    private val repository: DrugRepository
) {
    suspend operator fun invoke(bookmark: Bookmark) {
        repository.addBookmark(bookmark)
    }
}

