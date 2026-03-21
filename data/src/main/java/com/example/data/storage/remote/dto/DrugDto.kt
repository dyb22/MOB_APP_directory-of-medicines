package com.example.data.storage.remote.dto

import com.google.gson.annotations.SerializedName

/** массив results или null */
data class DrugSearchResponseDto(
    @SerializedName("results") val results: List<DrugResultDto>?
)

/** Элемент результата */
data class DrugResultDto(
    @SerializedName("id") val id: String,
    @SerializedName("openfda") val openFda: OpenFdaDto?,
    @SerializedName("description") val description: List<String>?,
    @SerializedName("indications_and_usage") val indicationsAndUsage: List<String>?
)

/** Вложенный объект: массивы brand_name, manufacturer_name */
data class OpenFdaDto(
    @SerializedName("brand_name") val brandName: List<String>?,
    @SerializedName("manufacturer_name") val manufacturerName: List<String>?
)
