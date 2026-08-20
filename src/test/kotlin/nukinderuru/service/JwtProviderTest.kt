package nukinderuru.service

import nukinderuru.domain.model.User
import nukinderuru.domain.service.JwtProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import java.util.UUID

class JwtProviderTest {
    private val jwtProvider = JwtProvider()
    private val user = User(
        id = UUID.fromString("00000000-0000-0000-0000-000000000123"),
        login = "tester",
        password = "secret"
    )

    @Test
    fun `generateAccessToken should produce valid access token with user claims`() {
        val accessToken = jwtProvider.generateAccessToken(user)

        val claims = jwtProvider.getClaims(accessToken)

        assertTrue(jwtProvider.validateAccessToken(accessToken))
        assertFalse(jwtProvider.validateRefreshToken(accessToken))
        assertEquals(user.id, jwtProvider.getUserId(accessToken))
        assertEquals(user.id.toString(), claims.get("userId", String::class.java))
        assertEquals("access", claims.get("type", String::class.java))
        assertNotNull(claims.issuedAt)
        assertNotNull(claims.expiration)
        assertTrue(claims.expiration.after(claims.issuedAt))
    }

    @Test
    fun `generateRefreshToken should produce valid refresh token with user claims`() {
        val refreshToken = jwtProvider.generateRefreshToken(user)

        val claims = jwtProvider.getClaims(refreshToken)

        assertTrue(jwtProvider.validateRefreshToken(refreshToken))
        assertFalse(jwtProvider.validateAccessToken(refreshToken))
        assertEquals(user.id, jwtProvider.getUserId(refreshToken))
        assertEquals("refresh", claims.get("type", String::class.java))
    }

    @Test
    fun `validate token should return false for malformed token`() {
        assertFalse(jwtProvider.validateAccessToken("not-a-jwt"))
        assertFalse(jwtProvider.validateRefreshToken("not-a-jwt"))
    }
}
