package com.example.data.repository

import android.content.Context
import com.example.data.mapper.toDomain
import com.example.data.network.OpenFdaApiFactory
import com.example.data.storage.remote.api.DrugApiService
import com.example.domain.model.Bookmark
import com.example.domain.model.Drug
import com.example.domain.model.SearchHistoryItem
import com.example.domain.repository.DrugRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.UUID

class DrugRepositoryImpl(
    private val appContext: Context,
    private val openFdaApi: DrugApiService = OpenFdaApiFactory.drugApiService,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : DrugRepository {

    // --- Guest local storage (when user is NOT logged in) ---
    private data class GuestBookmarkFolderDto(
        val id: String,
        val name: String,
        val createdAtMillis: Long
    )

    private data class GuestBookmarkDrugDto(
        val folderId: String,
        val drug: Drug
    )

    private data class GuestState(
        val bookmarksFolders: MutableList<GuestBookmarkFolderDto> = mutableListOf(),
        val bookmarkDrugs: MutableList<GuestBookmarkDrugDto> = mutableListOf(),
        val searchHistory: MutableList<SearchHistoryItem> = mutableListOf()
    )

    private val gson = Gson()
    private val guestFile = File(appContext.filesDir, "guest_data.json")
    private val guestMutex = Mutex()
    private var guestCache: GuestState? = null

    private suspend fun getGuestStateLocked(): GuestState {
        return withContext(Dispatchers.IO) {
            if (!guestFile.exists()) return@withContext GuestState()
            val json = guestFile.readText()
            runCatching { gson.fromJson(json, GuestState::class.java) }.getOrNull() ?: GuestState()
        }
    }

    private suspend fun saveGuestStateLocked(state: GuestState) {
        withContext(Dispatchers.IO) {
            guestFile.writeText(gson.toJson(state))
        }
    }

    private suspend fun getGuestState(): GuestState = guestMutex.withLock {
        guestCache ?: getGuestStateLocked().also { guestCache = it }
    }

    private suspend fun updateGuestState(block: (GuestState) -> Unit) {
        guestMutex.withLock {
            val state = guestCache ?: getGuestStateLocked()
            block(state)
            guestCache = state
            saveGuestStateLocked(state)
        }
    }

    // --- Search cache (in-memory) ---
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
            if (e.isNetworkError()) throw e
            emptyList()
        }
    }

    override suspend fun getDrugById(id: String): Drug? = searchCache[id]

    private fun Throwable.isNetworkError(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            if (current is UnknownHostException ||
                current is ConnectException ||
                current is SocketTimeoutException
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }

    override suspend fun getBookmarks(): List<Bookmark> {
        val doc = userDoc()
        return if (doc == null) {
            val guest = getGuestState()
            guest.bookmarksFolders.map { Bookmark(id = it.id, name = it.name) }
        } else {
            withContext(Dispatchers.IO) {
                val snapshot = doc.collection("bookmarksFolders").get().await()
                snapshot.documents.map { d ->
                    Bookmark(id = d.id, name = d.getString("name") ?: "")
                }
            }
        }
    }

    override suspend fun getDrugsInBookmark(bookmarkId: String): List<Drug> {
        val doc = userDoc()
        return if (doc == null) {
            val guest = getGuestState()
            guest.bookmarkDrugs.filter { it.folderId == bookmarkId }.map { it.drug }
        } else {
            withContext(Dispatchers.IO) {
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
    }

    override suspend fun addBookmark(bookmark: Bookmark) {
        val doc = userDoc()
        if (doc == null) {
            updateGuestState { guest ->
                val id = bookmark.id.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
                if (guest.bookmarksFolders.any { it.id == id }) return@updateGuestState
                guest.bookmarksFolders.add(
                    GuestBookmarkFolderDto(
                        id = id,
                        name = bookmark.name,
                        createdAtMillis = System.currentTimeMillis()
                    )
                )
            }
            return
        }

        withContext(Dispatchers.IO) {
            val id = bookmark.id.takeIf { it.isNotBlank() } ?: doc.collection("bookmarksFolders").document().id
            doc.collection("bookmarksFolders").document(id)
                .set(
                    mapOf(
                        "name" to bookmark.name,
                        "createdAtMillis" to System.currentTimeMillis()
                    )
                )
                .await()
        }
    }

    override suspend fun addDrugToBookmark(bookmarkId: String, drug: Drug) {
        val doc = userDoc()
        if (doc == null) {
            updateGuestState { guest ->
                val folderExists = guest.bookmarksFolders.any { it.id == bookmarkId }
                if (!folderExists) return@updateGuestState

                // Не дублируем один и тот же препарат в одной папке
                val alreadyExists = guest.bookmarkDrugs.any { it.folderId == bookmarkId && it.drug.id == drug.id }
                if (alreadyExists) return@updateGuestState

                guest.bookmarkDrugs.add(
                    GuestBookmarkDrugDto(
                        folderId = bookmarkId,
                        drug = drug
                    )
                )
            }
            return
        }

        withContext(Dispatchers.IO) {
            doc.collection("bookmarkDrugs").add(
                mapOf(
                    "folderId" to bookmarkId,
                    "drugId" to drug.id,
                    "name" to drug.name,
                    "manufacturer" to drug.manufacturer,
                    "price" to drug.price,
                    "description" to drug.description
                )
            ).await()
        }
    }

    override suspend fun removeBookmark(id: String) {
        val doc = userDoc()
        if (doc == null) {
            updateGuestState { guest ->
                guest.bookmarksFolders.removeAll { it.id == id }
                guest.bookmarkDrugs.removeAll { it.folderId == id }
            }
            return
        }

        withContext(Dispatchers.IO) {
            doc.collection("bookmarksFolders").document(id).delete().await()
            val drugsSnapshot = doc.collection("bookmarkDrugs").whereEqualTo("folderId", id).get().await()
            drugsSnapshot.documents.forEach { it.reference.delete().await() }
        }
    }

    override suspend fun renameBookmark(bookmarkId: String, newName: String) {
        val doc = userDoc()
        if (doc == null) {
            updateGuestState { guest ->
                val idx = guest.bookmarksFolders.indexOfFirst { it.id == bookmarkId }
                if (idx != -1) {
                    // Guest DTO использует val, поэтому обновляем через copy().
                    guest.bookmarksFolders[idx] = guest.bookmarksFolders[idx].copy(name = newName)
                }
            }
            return
        }

        withContext(Dispatchers.IO) {
            // Обновляем только поле "name" у папки закладок.
            doc.collection("bookmarksFolders")
                .document(bookmarkId)
                .update("name", newName)
                .await()
        }
    }

    override suspend fun getSearchHistory(): List<SearchHistoryItem> {
        val doc = userDoc()
        return if (doc == null) {
            val guest = getGuestState()
            guest.searchHistory.toList()
        } else {
            withContext(Dispatchers.IO) {
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
    }

    override suspend fun addToSearchHistory(drug: Drug) {
        val doc = userDoc()
        if (doc == null) {
            updateGuestState { guest ->
                guest.searchHistory.add(
                    SearchHistoryItem(
                        drug = drug,
                        viewedAtMillis = System.currentTimeMillis()
                    )
                )
            }
            return
        }

        withContext(Dispatchers.IO) {
            doc.collection("searchHistory").add(
                mapOf(
                    "drugId" to drug.id,
                    "name" to drug.name,
                    "manufacturer" to drug.manufacturer,
                    "price" to drug.price,
                    "description" to drug.description,
                    "viewedAtMillis" to System.currentTimeMillis()
                )
            ).await()
        }
    }

    override suspend fun clearSearchHistory() {
        val doc = userDoc()
        if (doc == null) {
            updateGuestState { guest ->
                guest.searchHistory.clear()
            }
            return
        }

        withContext(Dispatchers.IO) {
            val snapshot = doc.collection("searchHistory").get().await()
            snapshot.documents.forEach { it.reference.delete().await() }
        }
    }
}
