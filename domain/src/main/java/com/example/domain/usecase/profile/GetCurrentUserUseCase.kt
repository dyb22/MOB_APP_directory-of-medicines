package com.example.domain.usecase.profile

import com.example.domain.model.User
import com.example.domain.repository.UserRepository

/** Текущий авторизованный пользователь или null. */
class GetCurrentUserUseCase(
    private val repository: UserRepository
) {
    suspend operator fun invoke(): User? {
        return repository.getCurrentUser()
    }
}

