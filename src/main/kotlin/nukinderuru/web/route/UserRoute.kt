package nukinderuru.web.route

import nukinderuru.common.ValidationMessages
import nukinderuru.domain.service.UserService
import nukinderuru.web.model.ErrorResponse
import nukinderuru.web.model.WebUser
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.userRoute() {
    val userService by inject<UserService>()

    get("/user") {
        val userId = call.principal<UserIdPrincipal>()?.name?.let(UUID::fromString)
            ?: return@get call.respond(HttpStatusCode.Unauthorized)

        val user = userService.getUserById(userId)
            ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse(ValidationMessages.USER_NOT_FOUND))

        call.respond(HttpStatusCode.OK, WebUser(id = user.id.toString(), login = user.login))
    }

    get("/user/{userId}") {
        val userId = call.parameters["userId"]?.let(UUID::fromString)
            ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse(ValidationMessages.USER_UUID_IS_REQUIRED))

        val user = userService.getUserById(userId)
            ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse(ValidationMessages.USER_NOT_FOUND))

        call.respond(HttpStatusCode.OK, WebUser(id = user.id.toString(), login = user.login))
    }
}
