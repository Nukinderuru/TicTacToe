package nukinderuru.domain.service

import nukinderuru.web.model.JwtRequest
import nukinderuru.web.model.JwtResponse
import nukinderuru.web.model.RefreshJwtRequest
import nukinderuru.web.model.SignUpRequest

class JwtAuthService(
    private val userService: UserService,
    private val jwtProvider: JwtProvider
) : AuthService {
    override fun register(signUpRequest: SignUpRequest): Boolean = userService.register(signUpRequest)

    override fun login(jwtRequest: JwtRequest): JwtResponse? {
        val user = userService.getUserByLogin(jwtRequest.login) ?: return null
        if (user.password != jwtRequest.password) {
            return null
        }

        return JwtResponse(
            type = "Bearer",
            accessToken = jwtProvider.generateAccessToken(user),
            refreshToken = jwtProvider.generateRefreshToken(user)
        )
    }

    override fun refreshAccessToken(refreshJwtRequest: RefreshJwtRequest): JwtResponse? {
        val refreshToken = refreshJwtRequest.refreshToken
        if (!jwtProvider.validateRefreshToken(refreshToken)) {
            return null
        }

        val user = userService.getUserById(jwtProvider.getUserId(refreshToken)) ?: return null
        return JwtResponse(
            type = "Bearer",
            accessToken = jwtProvider.generateAccessToken(user),
            refreshToken = refreshToken
        )
    }

    override fun refreshRefreshToken(refreshJwtRequest: RefreshJwtRequest): JwtResponse? {
        val refreshToken = refreshJwtRequest.refreshToken
        if (!jwtProvider.validateRefreshToken(refreshToken)) {
            return null
        }

        val user = userService.getUserById(jwtProvider.getUserId(refreshToken)) ?: return null
        return JwtResponse(
            type = "Bearer",
            accessToken = jwtProvider.generateAccessToken(user),
            refreshToken = jwtProvider.generateRefreshToken(user)
        )
    }
}
