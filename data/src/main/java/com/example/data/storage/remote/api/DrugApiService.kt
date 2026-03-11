package com.example.data.storage.remote.api

import com.example.data.storage.remote.dto.DrugSearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface DrugApiService {

    @GET("drug/label.json")
    suspend fun searchDrugs(
        @Query("search") search: String,
        @Query("limit") limit: Int = 20
    ): DrugSearchResponseDto
}
