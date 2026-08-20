package nukinderuru.domain.service

import nukinderuru.domain.model.User
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

class JwtProvider {
    fun generateAccessToken(user: User): String = buildToken(user, ACCESS_TOKEN_TYPE, ACCESS_TOKEN_LIFETIME)

    fun generateRefreshToken(user: User): String = buildToken(user, REFRESH_TOKEN_TYPE, REFRESH_TOKEN_LIFETIME)

    fun validateAccessToken(token: String): Boolean = validateToken(token, ACCESS_TOKEN_TYPE)

    fun validateRefreshToken(token: String): Boolean = validateToken(token, REFRESH_TOKEN_TYPE)

    fun getClaims(token: String): Claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).payload

    fun getUserId(token: String): UUID = UUID.fromString(getClaims(token).get(USER_ID_CLAIM, String::class.java))

    private fun buildToken(user: User, tokenType: String, lifetime: Duration): String {
        val issuedAt = Instant.now()
        val expiresAt = issuedAt.plus(lifetime)

        return Jwts.builder()
            .claim(USER_ID_CLAIM, user.id.toString())
            .claim(TOKEN_TYPE_CLAIM, tokenType)
            .issuedAt(issuedAt.toDate())
            .expiration(expiresAt.toDate())
            .signWith(secretKey)
            .compact()
    }

    private fun Instant.toDate(): Date = Date.from(this)

    private fun validateToken(token: String, tokenType: String): Boolean = try {
        getClaims(token).get(TOKEN_TYPE_CLAIM, String::class.java) == tokenType
    } catch (_: Exception) {
        false
    }

    private companion object {
        private const val USER_ID_CLAIM = "userId"
        private const val TOKEN_TYPE_CLAIM = "type"
        private const val ACCESS_TOKEN_TYPE = "access"
        private const val REFRESH_TOKEN_TYPE = "refresh"
        private val ACCESS_TOKEN_LIFETIME: Duration = Duration.ofMinutes(5)
        private val REFRESH_TOKEN_LIFETIME: Duration = Duration.ofDays(1)
        private val secretKey: SecretKey = Keys.hmacShaKeyFor(
            "nukinderuru-tictactoe-jwt-secret-key-2026".toByteArray(StandardCharsets.UTF_8),
        )
    }
}
