package nukinderuru.web.route

import nukinderuru.common.ValidationMessages
import nukinderuru.domain.service.AuthService
import nukinderuru.web.model.ErrorResponse
import nukinderuru.web.model.SignUpRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import org.koin.ktor.ext.inject

fun Route.registrationRoute() {
    val authService by inject<AuthService>()

    post("/signup") {
        val signUpRequest = try {
            call.receive<SignUpRequest>()
        } catch (_: Exception) {
            return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse(ValidationMessages.REQUEST_BODY_IS_INVALID))
        }

        if (!authService.register(signUpRequest)) {
            return@post call.respond(HttpStatusCode.Conflict, ErrorResponse(ValidationMessages.USER_WITH_LOGIN_EXISTS))
        }

        call.respond(HttpStatusCode.Created)
    }
}
