package com.example.domain.repository

import com.example.domain.model.Bookmark
import com.example.domain.model.Drug
import com.example.domain.model.SearchHistoryItem

interface DrugRepository {
    suspend fun searchDrugs(query: String): List<Drug>
    suspend fun getDrugById(id: String): Drug?

    suspend fun getBookmarks(): List<Bookmark>
    suspend fun getDrugsInBookmark(bookmarkId: String): List<Drug>
    suspend fun addBookmark(bookmark: Bookmark)
    suspend fun addDrugToBookmark(bookmarkId: String, drug: Drug)
    suspend fun removeBookmark(id: String)
    suspend fun renameBookmark(bookmarkId: String, newName: String)

    suspend fun getSearchHistory(): List<SearchHistoryItem>
    suspend fun addToSearchHistory(drug: Drug)
    suspend fun clearSearchHistory()
}

