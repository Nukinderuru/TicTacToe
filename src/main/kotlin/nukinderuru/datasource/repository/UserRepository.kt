package nukinderuru.datasource.repository

import nukinderuru.domain.model.User
import java.util.UUID

interface UserRepository {
    fun saveUser(user: User): Boolean

    fun fetchUserByLogin(login: String): User?

    fun fetchUserById(userId: UUID): User?
}
