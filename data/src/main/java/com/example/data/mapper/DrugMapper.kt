package com.example.data.mapper

import com.example.data.storage.local.entity.DrugEntity
import com.example.data.storage.remote.dto.DrugResultDto
import com.example.domain.model.Drug

fun DrugResultDto.toDomain(): Drug {
    val brand = openFda?.brandName?.firstOrNull()
    val manufacturer = openFda?.manufacturerName?.firstOrNull()

    return Drug(
        id = id,
        name = brand ?: "",
        manufacturer = manufacturer ?: "",
        price = "",
        description = ""
    )
}

fun DrugEntity.toDomain(): Drug = Drug(
    id = id,
    name = name,
    manufacturer = "", // локальная сущность пока не хранит производителя
    price = "",
    description = ""
)

