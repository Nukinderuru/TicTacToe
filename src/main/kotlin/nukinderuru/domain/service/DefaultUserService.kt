package nukinderuru.domain.service

import nukinderuru.common.ValidationMessages
import nukinderuru.datasource.repository.UserRepository
import nukinderuru.domain.model.User
import nukinderuru.web.model.SignUpRequest
import java.util.UUID

class DefaultUserService(
    private val userRepository: UserRepository,
) : UserService {
    override fun register(signUpRequest: SignUpRequest): Boolean {
        require(signUpRequest.login.isNotBlank()) { ValidationMessages.LOGIN_MUST_NOT_BE_BLANK }
        require(signUpRequest.password.isNotBlank()) { ValidationMessages.PASSWORD_MUST_NOT_BE_BLANK }

        return userRepository.saveUser(
            User(
                id = UUID.randomUUID(),
                login = signUpRequest.login,
                password = signUpRequest.password,
            ),
        )
    }

    override fun getUserByLogin(login: String): User? = userRepository.fetchUserByLogin(login)

    override fun getUserById(userId: UUID): User? = userRepository.fetchUserById(userId)
}
