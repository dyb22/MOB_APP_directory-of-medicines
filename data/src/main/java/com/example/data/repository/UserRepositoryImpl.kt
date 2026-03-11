package com.example.data.repository

import com.example.domain.model.User
import com.example.domain.repository.UserRepository

class UserRepositoryImpl : UserRepository {
    override suspend fun getCurrentUser(): User? = null

    override suspend fun login(email: String, password: String): User {
        // TODO: replace with real auth
        return User(id = "stub", name = email.substringBefore("@"))
    }

    override suspend fun logout() {
        // TODO: clear persisted auth
    }
}

