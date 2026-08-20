package nukinderuru.domain.service

import nukinderuru.domain.model.User
import nukinderuru.web.model.SignUpRequest
import java.util.UUID

interface UserService {
    fun register(signUpRequest: SignUpRequest): Boolean

    fun getUserByLogin(login: String): User?

    fun getUserById(userId: UUID): User?
}
