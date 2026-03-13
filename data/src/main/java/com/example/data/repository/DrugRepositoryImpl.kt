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

/**
 * Реализация через OpenFDA Drug Label API.
 * Название, производитель, описание; цена "0 рублей".
 */
class DrugRepositoryImpl(
    private val openFdaApi: DrugApiService = OpenFdaApiFactory.drugApiService
) : DrugRepository {

    private val searchCache = mutableMapOf<String, Drug>()

    override suspend fun searchDrugs(query: String): List<Drug> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList<Drug>()
        val term = query.trim().replace("*", "")
        if (term.isEmpty()) return@withContext emptyList<Drug>()
        val search = "openfda.brand_name:$term*"
        try {
            val response = openFdaApi.searchDrugs(search = search, limit = 20)
            val results = response.results ?: return@withContext emptyList<Drug>()
            val list = results.map { it.toDomain() }
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

    override suspend fun getBookmarks(): List<Bookmark> = emptyList()

    override suspend fun addBookmark(bookmark: Bookmark) {}

    override suspend fun removeBookmark(id: String) {}

    override suspend fun getSearchHistory(): List<SearchHistoryItem> = emptyList()

    override suspend fun clearSearchHistory() {}
}
