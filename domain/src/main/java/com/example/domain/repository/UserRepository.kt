package com.example.domain.repository

import com.example.domain.model.User

interface UserRepository {
    suspend fun getCurrentUser(): User?
    suspend fun login(email: String, password: String): User
    suspend fun register(email: String, password: String): User
    suspend fun logout()
}

