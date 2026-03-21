package com.example.domain.usecase.profile

import com.example.domain.model.User
import com.example.domain.repository.UserRepository

/** Вход по email и паролю. Возвращает User или выбрасывает исключение. */
class LoginUseCase(
    private val repository: UserRepository
) {
    suspend operator fun invoke(email: String, password: String): User {
        return repository.login(email, password)
    }
}

