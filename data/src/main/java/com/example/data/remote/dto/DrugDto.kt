package com.example.data.storage.remote.dto

import com.google.gson.annotations.SerializedName

data class DrugSearchResponseDto(
    @SerializedName("results") val results: List<DrugResultDto>?
)

data class DrugResultDto(
    @SerializedName("id") val id: String,
    @SerializedName("openfda") val openFda: OpenFdaDto?
)

data class OpenFdaDto(
    @SerializedName("brand_name") val brandName: List<String>?,
    @SerializedName("manufacturer_name") val manufacturerName: List<String>?
)

