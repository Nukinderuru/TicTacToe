package nukinderuru.web.module

import nukinderuru.common.ValidationMessages
import nukinderuru.datasource.table.CurrentGamesTable
import nukinderuru.datasource.table.UsersTable
import nukinderuru.di.applicationModule
import nukinderuru.domain.service.JwtProvider
import nukinderuru.web.route.authorizationRoute
import nukinderuru.web.route.frontendRoute
import nukinderuru.web.route.userRoute
import nukinderuru.web.route.registrationRoute
import nukinderuru.web.model.ErrorResponse
import nukinderuru.web.route.gameRoute
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.bearer
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import org.koin.ktor.plugin.Koin
import org.koin.ktor.ext.inject
import org.koin.logger.slf4jLogger
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabase() {
    val config = environment.config.config("ktor.database")

    Database.connect(
        url = config.property("url").getString(),
        driver = config.property("driver").getString(),
        user = config.property("user").getString(),
        password = config.property("password").getString(),
    )

    connectToDatabaseWithRetry()
}

private fun Application.connectToDatabaseWithRetry() {
    var lastError: Exception? = null

    repeat(15) { attempt ->
        try {
            transaction {
                SchemaUtils.create(CurrentGamesTable, UsersTable)
            }
            return
        } catch (exception: Exception) {
            lastError = exception
            environment.log.warn("Database is not ready yet. Retry ${attempt + 1}/15 in 2s.", exception)
            Thread.sleep(2_000)
        }
    }

    throw IllegalStateException(ValidationMessages.DATABASE_CONNECTION_FAILED, lastError)
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json()
    }
}

fun Application.configureAuthentication() {
    val jwtProvider by inject<JwtProvider>()

    install(Authentication) {
        bearer("auth-bearer") {
            authenticate { tokenCredential ->
                tokenCredential.token.takeIf(jwtProvider::validateAccessToken)?.let { accessToken ->
                    UserIdPrincipal(jwtProvider.getUserId(accessToken).toString())
                }
            }
        }
    }
}

fun Application.configureMonitoring() {
    install(CallLogging)
    install(Koin) {
        slf4jLogger()
        modules(applicationModule)
    }
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message ?: ValidationMessages.INVALID_REQUEST))
        }
    }
}

fun Application.configureRouting() {
    routing {
        registrationRoute()
        authorizationRoute()
        authenticate("auth-bearer") {
            gameRoute()
            userRoute()
        }
        frontendRoute()
    }
}
