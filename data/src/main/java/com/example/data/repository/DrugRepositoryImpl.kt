package com.example.data.repository

import com.example.data.mapper.toDomain
import com.example.data.network.OpenFdaApiFactory
import com.example.data.storage.remote.api.DrugApiService
import com.example.domain.model.Bookmark
import com.example.domain.model.Drug
import com.example.domain.model.SearchHistoryItem
import com.example.domain.repository.DrugRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Реализация: поиск через OpenFDA; закладки и история только у авторизованного пользователя (Firestore).
 * Без входа закладки и история не показываются и не сохраняются.
 */
class DrugRepositoryImpl(
    private val openFdaApi: DrugApiService = OpenFdaApiFactory.drugApiService,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : DrugRepository {

    private val searchCache = mutableMapOf<String, Drug>()

    private fun userDoc() = auth.currentUser?.let { firestore.collection("users").document(it.uid) }

    override suspend fun searchDrugs(query: String): List<Drug> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList<Drug>()
        val term = query.trim().replace("*", "")
        if (term.isEmpty()) return@withContext emptyList<Drug>()
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

    override suspend fun getDrugById(id: String): Drug? = searchCache[id]

    override suspend fun getBookmarks(): List<Bookmark> {
        val doc = userDoc() ?: return emptyList()
        return withContext(Dispatchers.IO) {
            val snapshot = doc.collection("bookmarksFolders").get().await()
            snapshot.documents.map { d ->
                Bookmark(id = d.id, name = d.getString("name") ?: "")
            }
        }
    }

    override suspend fun getDrugsInBookmark(bookmarkId: String): List<Drug> {
        val doc = userDoc() ?: return emptyList()
        return withContext(Dispatchers.IO) {
            val snapshot = doc.collection("bookmarkDrugs")
                .whereEqualTo("folderId", bookmarkId)
                .get()
                .await()
            snapshot.documents.map { d ->
                Drug(
                    id = d.getString("drugId") ?: d.id,
                    name = d.getString("name") ?: "",
                    manufacturer = d.getString("manufacturer") ?: "",
                    price = d.getString("price") ?: "",
                    description = d.getString("description") ?: ""
                )
            }
        }
    }

    override suspend fun addBookmark(bookmark: Bookmark) {
        val doc = userDoc() ?: return
        withContext(Dispatchers.IO) {
            val id = bookmark.id.takeIf { it.isNotBlank() } ?: doc.collection("bookmarksFolders").document().id
            doc.collection("bookmarksFolders").document(id)
                .set(mapOf(
                    "name" to bookmark.name,
                    "createdAtMillis" to System.currentTimeMillis()
                ))
                .await()
        }
    }

    override suspend fun addDrugToBookmark(bookmarkId: String, drug: Drug) {
        val doc = userDoc() ?: return
        withContext(Dispatchers.IO) {
            doc.collection("bookmarkDrugs").add(mapOf(
                "folderId" to bookmarkId,
                "drugId" to drug.id,
                "name" to drug.name,
                "manufacturer" to drug.manufacturer,
                "price" to drug.price,
                "description" to drug.description
            )).await()
        }
    }

    override suspend fun removeBookmark(id: String) {
        val doc = userDoc() ?: return
        withContext(Dispatchers.IO) {
            doc.collection("bookmarksFolders").document(id).delete().await()
            val drugsSnapshot = doc.collection("bookmarkDrugs").whereEqualTo("folderId", id).get().await()
            drugsSnapshot.documents.forEach { it.reference.delete().await() }
        }
    }

    override suspend fun getSearchHistory(): List<SearchHistoryItem> {
        val doc = userDoc() ?: return emptyList()
        return withContext(Dispatchers.IO) {
            val snapshot = doc.collection("searchHistory")
                .orderBy("viewedAtMillis")
                .get()
                .await()
            snapshot.documents.mapNotNull { d ->
                val viewedAt = d.getLong("viewedAtMillis") ?: 0L
                val drugId = d.getString("drugId") ?: d.id
                SearchHistoryItem(
                    drug = Drug(
                        id = drugId,
                        name = d.getString("name") ?: "",
                        manufacturer = d.getString("manufacturer") ?: "",
                        price = d.getString("price") ?: "",
                        description = d.getString("description") ?: ""
                    ),
                    viewedAtMillis = viewedAt
                )
            }
        }
    }

    override suspend fun addToSearchHistory(drug: Drug) {
        val doc = userDoc() ?: return
        withContext(Dispatchers.IO) {
            doc.collection("searchHistory").add(mapOf(
                "drugId" to drug.id,
                "name" to drug.name,
                "manufacturer" to drug.manufacturer,
                "price" to drug.price,
                "description" to drug.description,
                "viewedAtMillis" to System.currentTimeMillis()
            )).await()
        }
    }

    override suspend fun clearSearchHistory() {
        val doc = userDoc() ?: return
        withContext(Dispatchers.IO) {
            val snapshot = doc.collection("searchHistory").get().await()
            snapshot.documents.forEach { it.reference.delete().await() }
        }
    }
}
