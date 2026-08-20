package nukinderuru.domain.service

import nukinderuru.web.model.JwtRequest
import nukinderuru.web.model.JwtResponse
import nukinderuru.web.model.RefreshJwtRequest
import nukinderuru.web.model.SignUpRequest

interface AuthService {
    fun register(signUpRequest: SignUpRequest): Boolean

    fun login(jwtRequest: JwtRequest): JwtResponse?

    fun refreshAccessToken(refreshJwtRequest: RefreshJwtRequest): JwtResponse?

    fun refreshRefreshToken(refreshJwtRequest: RefreshJwtRequest): JwtResponse?
}
