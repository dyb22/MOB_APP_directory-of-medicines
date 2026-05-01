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
import com.google.firebase.firestore.Query
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

/**
 * Реализация DrugRepository. Поиск — OpenFDA API. Закладки и история — Firestore при входе,
 * локальный JSON (guest_data.json) для гостя. searchCache — кэш результатов поиска для getDrugById.
 */
class DrugRepositoryImpl(
    private val appContext: Context,
    private val openFdaApi: DrugApiService = OpenFdaApiFactory.drugApiService,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : DrugRepository {

    // Гость: закладки и история в JSON в filesDir
    /** DTO папки закладок для JSON гостя */
    private data class GuestBookmarkFolderDto(
        val id: String,
        val name: String,
        val createdAtMillis: Long
    )

    /** DTO связи папка-препарат для гостя */
    private data class GuestBookmarkDrugDto(
        val folderId: String,
        val drug: Drug
    )

    /** Состояние гостя: папки, препараты в папках, история поиска */
    private data class GuestState(
        val bookmarksFolders: MutableList<GuestBookmarkFolderDto> = mutableListOf(),
        val bookmarkDrugs: MutableList<GuestBookmarkDrugDto> = mutableListOf(),
        val searchHistory: MutableList<SearchHistoryItem> = mutableListOf()
    )

    private val gson = Gson()
    private val guestFile = File(appContext.filesDir, "guest_data.json")
    private val guestMutex = Mutex()
    private var guestCache: GuestState? = null

    /** Чтение guest_data.json с диска  */
    private suspend fun getGuestStateLocked(): GuestState {
        return withContext(Dispatchers.IO) {
            if (!guestFile.exists()) return@withContext GuestState()
            val json = guestFile.readText()
            runCatching { gson.fromJson(json, GuestState::class.java) }.getOrNull() ?: GuestState()
        }
    }

    /** Сохранение состояния гостя в JSON */
    private suspend fun saveGuestStateLocked(state: GuestState) {
        withContext(Dispatchers.IO) {
            guestFile.writeText(gson.toJson(state))
        }
    }

    /** Получить состояние с кэшем в памяти, под блокировкой */
    private suspend fun getGuestState(): GuestState = guestMutex.withLock {
        guestCache ?: getGuestStateLocked().also { guestCache = it }
    }

    /** Изменить состояние и сохранить на диск */
    private suspend fun updateGuestState(block: (GuestState) -> Unit) {
        guestMutex.withLock {
            val state = guestCache ?: getGuestStateLocked()
            block(state)
            guestCache = state
            saveGuestStateLocked(state)
        }
    }

    // После поиска: id -> Drug для getDrugById
    private val searchCache = mutableMapOf<String, Drug>()

    /** Ссылка на документ пользователя в Firestore или null для гостя */
    private fun userDoc() = auth.currentUser?.let { firestore.collection("users").document(it.uid) }

    /** Поиск по OpenFDA. Строка openfda.brand_name:term*. Кэширует результат для getDrugById. */
    override suspend fun searchDrugs(query: String): List<Drug> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList<Drug>()
        val term = query.trim().replace("*", "")
        if (term.isEmpty()) return@withContext emptyList<Drug>()
        val search = "openfda.brand_name:${term}*" // префиксный поиск по бренду
        try {
            val response = openFdaApi.searchDrugs(search = search, limit = 20)
            val results = response.results ?: emptyList()
            val list = results.map { dto -> dto.toDomain() }
            searchCache.clear()
            list.forEach { searchCache[it.id] = it }
            list
        } catch (e: Exception) {
            searchCache.clear()
            if (e.isNetworkError()) throw e // сеть — наверх для UI
            emptyList()
        }
    }

    /** Препарат из кэша последнего поиска. null если не искали или id нет в результатах. */
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

    /** Папки закладок. Гость: из guest_data. Пользователь: Firestore bookmarksFolders. */
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

    /** Препараты в папке. Гость: из bookmarkDrugs. Пользователь: Firestore bookmarkDrugs по folderId. */
    override suspend fun getDrugsInBookmark(bookmarkId: String): List<Drug> {
        val doc = userDoc()
        return if (doc == null) {
            val guest = getGuestState()
            guest.bookmarkDrugs
                .filter { it.folderId == bookmarkId }
                .map { it.drug }
                .asReversed()
        } else {
            withContext(Dispatchers.IO) {
                val snapshot = doc.collection("bookmarkDrugs")
                    .whereEqualTo("folderId", bookmarkId)
                    .get()
                    .await()
                snapshot.documents
                    .sortedByDescending { it.getLong("addedAtMillis") ?: 0L }
                    .map { d ->
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

    /** Создание папки. Гость: UUID при пустом id. Пользователь: Firestore document. */
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

    /** Добавление препарата в папку. Дубликаты не добавляются. */
    override suspend fun addDrugToBookmark(bookmarkId: String, drug: Drug) {
        val doc = userDoc()
        if (doc == null) {
            updateGuestState { guest ->
                val folderExists = guest.bookmarksFolders.any { it.id == bookmarkId }
                if (!folderExists) return@updateGuestState

                val alreadyExists = guest.bookmarkDrugs.any { it.folderId == bookmarkId && it.drug.id == drug.id }
                if (alreadyExists) return@updateGuestState // без дубликата в папке

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
                    "description" to drug.description,
                    "addedAtMillis" to System.currentTimeMillis()
                )
            ).await()
        }
    }

    /** Удаление папки и всех препаратов в ней. */
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

    /** Переименование папки. Гость: copy DTO. Пользователь: Firestore update. */
    override suspend fun renameBookmark(bookmarkId: String, newName: String) {
        val doc = userDoc()
        if (doc == null) {
            updateGuestState { guest ->
                val idx = guest.bookmarksFolders.indexOfFirst { it.id == bookmarkId }
                if (idx != -1) {
                    guest.bookmarksFolders[idx] = guest.bookmarksFolders[idx].copy(name = newName) // data class val
                }
            }
            return
        }

        withContext(Dispatchers.IO) {
            doc.collection("bookmarksFolders")
                .document(bookmarkId)
                .update("name", newName)
                .await()
        }
    }

    /** История просмотров по viewedAtMillis. Гость: из JSON. Пользователь: Firestore searchHistory. */
    override suspend fun getSearchHistory(): List<SearchHistoryItem> {
        val doc = userDoc()
        return if (doc == null) {
            val guest = getGuestState()
            guest.searchHistory.sortedByDescending { it.viewedAtMillis }
        } else {
            withContext(Dispatchers.IO) {
                val snapshot = doc.collection("searchHistory")
                    .orderBy("viewedAtMillis", Query.Direction.DESCENDING)
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

    /** Добавление препарата в историю при открытии карточки с поиска. */
    override suspend fun addToSearchHistory(drug: Drug) {
        val doc = userDoc()
        if (doc == null) {
            updateGuestState { guest ->
                guest.searchHistory.add(
                    0,
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

    /** Очистка истории. Гость: clear списка. Пользователь: удаление документов Firestore. */
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
