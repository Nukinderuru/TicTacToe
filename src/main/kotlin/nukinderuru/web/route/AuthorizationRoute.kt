package nukinderuru.web.route

import nukinderuru.domain.service.AuthService
import nukinderuru.web.model.JwtRequest
import nukinderuru.web.model.RefreshJwtRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import org.koin.ktor.ext.inject

fun Route.authorizationRoute() {
    val authService by inject<AuthService>()

    post("/login") {
        val jwtRequest = call.receive<JwtRequest>()
        val response = authService.login(jwtRequest)
            ?: return@post call.respond(HttpStatusCode.Unauthorized)

        call.respond(HttpStatusCode.OK, response)
    }

    post("/login/access-token") {
        val refreshJwtRequest = call.receive<RefreshJwtRequest>()
        val response = authService.refreshAccessToken(refreshJwtRequest)
            ?: return@post call.respond(HttpStatusCode.Unauthorized)

        call.respond(HttpStatusCode.OK, response)
    }

    authenticate("auth-bearer") {
        post("/login/refresh-token") {
            val refreshJwtRequest = call.receive<RefreshJwtRequest>()
            val response = authService.refreshRefreshToken(refreshJwtRequest)
                ?: return@post call.respond(HttpStatusCode.Unauthorized)

            call.respond(HttpStatusCode.OK, response)
        }
    }
}
