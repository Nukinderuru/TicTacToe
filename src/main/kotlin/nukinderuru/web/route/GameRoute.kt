package nukinderuru.web.route

import nukinderuru.common.ValidationMessages
import nukinderuru.domain.service.GameService
import nukinderuru.domain.service.UserService
import nukinderuru.web.mapper.toWebModel
import nukinderuru.web.model.CreateGameRequest
import nukinderuru.web.model.ErrorResponse
import nukinderuru.web.model.WebMoveRequest
import nukinderuru.web.model.WebTopPlayer
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import nukinderuru.web.mapper.toDomainModel
import java.util.UUID
import org.koin.ktor.ext.inject

fun Route.gameRoute() {
    val gameService by inject<GameService>()
    val userService by inject<UserService>()

    get("/game") {
        val userId = call.currentUserId()
        call.respond(HttpStatusCode.OK, gameService.getAvailableGames(userId).map { it.toWebModel() })
    }

    get("/game/history") {
        call.respond(HttpStatusCode.OK, gameService.getCompletedGames(call.currentUserId()).map { it.toWebModel() })
    }

    get("/game/leaderboard") {
        val limit = call.request.queryParameters["limit"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse(ValidationMessages.INVALID_REQUEST))

        val leaderboard = gameService.getTopPlayers(limit).mapNotNull { player ->
            userService.getUserById(player.userId)?.let { user ->
                WebTopPlayer(
                    userId = user.id.toString(),
                    login = user.login,
                    winRatio = player.winRatio,
                )
            }
        }
        call.respond(HttpStatusCode.OK, leaderboard)
    }

    post("/game") {
        val createGameRequest = try {
            call.receive<CreateGameRequest>()
        } catch (_: Exception) {
            return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse(ValidationMessages.REQUEST_BODY_IS_INVALID))
        }

        val createdGame = gameService.createNewGame(call.currentUserId(), createGameRequest.toDomainModel())
        call.respond(HttpStatusCode.Created, createdGame.toWebModel())
    }

    get("/game/{gameId}") {
        val gameId = call.parameters["gameId"]?.let(UUID::fromString)
            ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse(ValidationMessages.GAME_UUID_IS_REQUIRED))

        val game = gameService.getCurrentGame(gameId)
            ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse(ValidationMessages.GAME_NOT_FOUND))

        call.respond(HttpStatusCode.OK, game.toWebModel())
    }

    post("/game/{gameId}/join") {
        val gameId = call.parameters["gameId"]?.let(UUID::fromString)
            ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse(ValidationMessages.GAME_UUID_IS_REQUIRED))

        val joinedGame = gameService.joinGame(gameId, call.currentUserId())
        call.respond(HttpStatusCode.OK, joinedGame.toWebModel())
    }

    post("/game/{gameId}") {
        val gameId = call.parameters["gameId"]?.let(UUID::fromString)
            ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse(ValidationMessages.GAME_UUID_IS_REQUIRED))

        val moveRequest = try {
            call.receive<WebMoveRequest>()
        } catch (_: Exception) {
            return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse(ValidationMessages.REQUEST_BODY_IS_INVALID))
        }

        val updatedGame = gameService.makeMove(gameId, call.currentUserId(), moveRequest.rowIndex, moveRequest.columnIndex)
        call.respond(HttpStatusCode.OK, updatedGame.toWebModel())
    }
}

private fun ApplicationCall.currentUserId(): UUID = principal<UserIdPrincipal>()
    ?.name
    ?.let(UUID::fromString)
    ?: throw IllegalStateException(ValidationMessages.AUTHORIZED_USER_ID_IS_MISSING)
