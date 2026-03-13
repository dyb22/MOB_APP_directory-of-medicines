package com.example.data.repository

import com.example.data.mapper.toDomain
import com.example.data.network.OpenFdaApiFactory
import com.example.data.storage.remote.api.DrugApiService
import com.example.domain.model.Bookmark
import com.example.domain.model.Drug
import com.example.domain.model.SearchHistoryItem
import com.example.domain.repository.DrugRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class BookmarkFolder(
    val id: String,
    val name: String,
    val drugs: MutableList<Drug>
)

/**
 * Реализация через OpenFDA Drug Label API + простое хранение закладок в памяти.
 */
class DrugRepositoryImpl(
    private val openFdaApi: DrugApiService = OpenFdaApiFactory.drugApiService
) : DrugRepository {

    private val searchCache = mutableMapOf<String, Drug>()

    // Папки закладок и их содержимое храним в памяти
    private val bookmarkFolders = mutableListOf<BookmarkFolder>()

    // История просмотренных препаратов (только из экрана поиска)
    private val searchHistory = mutableListOf<SearchHistoryItem>()

    override suspend fun searchDrugs(query: String): List<Drug> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList<Drug>()
        val term = query.trim().replace("*", "")
        if (term.isEmpty()) return@withContext emptyList<Drug>()
        // OpenFDA не поддерживает ведущий wildcard, поэтому используем префиксный поиск
        val search = "openfda.brand_name:${term}*"
        try {
            val response = openFdaApi.searchDrugs(search = search, limit = 20)
            val results = response.results ?: emptyList()
            val list = results.map { dto -> dto.toDomain() }
            searchCache.clear()
            list.forEach { searchCache[it.id] = it }
            list
        } catch (e: Exception) {
            searchCache.clear()
            emptyList()
        }
    }

    override suspend fun getDrugById(id: String): Drug? =
        searchCache[id]

    override suspend fun getBookmarks(): List<Bookmark> =
        bookmarkFolders.map { Bookmark(id = it.id, name = it.name) }

    override suspend fun getDrugsInBookmark(bookmarkId: String): List<Drug> =
        bookmarkFolders.find { it.id == bookmarkId }?.drugs?.toList() ?: emptyList()

    override suspend fun addBookmark(bookmark: Bookmark) {
        // Если id пустой — генерируем простой строковый id
        val id = bookmark.id.takeIf { it.isNotBlank() } ?: (bookmarkFolders.size + 1).toString()
        if (bookmarkFolders.any { it.id == id }) return
        bookmarkFolders.add(BookmarkFolder(id = id, name = bookmark.name, drugs = mutableListOf()))
    }

    override suspend fun addDrugToBookmark(bookmarkId: String, drug: Drug) {
        val folder = bookmarkFolders.find { it.id == bookmarkId } ?: return
        if (folder.drugs.any { it.id == drug.id }) return
        folder.drugs.add(drug)
    }

    override suspend fun removeBookmark(id: String) {
        bookmarkFolders.removeAll { it.id == id }
    }

    override suspend fun getSearchHistory(): List<SearchHistoryItem> =
        searchHistory.sortedByDescending { it.viewedAtMillis }

    override suspend fun addToSearchHistory(drug: Drug) {
        // Удаляем старую запись для этого препарата, если была, и добавляем новую в начало
        searchHistory.removeAll { it.drug.id == drug.id }
        searchHistory.add(0, SearchHistoryItem(drug = drug, viewedAtMillis = System.currentTimeMillis()))
    }

    override suspend fun clearSearchHistory() {
        searchHistory.clear()
    }
}
