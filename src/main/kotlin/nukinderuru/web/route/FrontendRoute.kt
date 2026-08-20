package nukinderuru.web.route

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import java.io.File

fun Route.frontendRoute() {
    val distDirectory = resolveDistDirectory()
    val indexFile = File(distDirectory, "index.html")

    get("/") {
        if (!indexFile.exists()) {
            return@get call.respond(HttpStatusCode.ServiceUnavailable, "Frontend is not built. Run 'npm run build' in view/ first.")
        }

        call.response.header(HttpHeaders.CacheControl, "no-store, no-cache, must-revalidate")
        call.respondFile(indexFile)
    }

    get("/{path...}") {
        val requestedPath = call.parameters.getAll("path") ?: emptyList()
        if (requestedPath.firstOrNull() in setOf("game", "login", "signup", "user")) {
            return@get call.respond(HttpStatusCode.NotFound)
        }

        if (!indexFile.exists()) {
            return@get call.respond(HttpStatusCode.ServiceUnavailable, "Frontend is not built. Run 'npm run build' in view/ first.")
        }

        val requestedFile = File(distDirectory, requestedPath.joinToString(File.separator)).canonicalFile
        if (requestedFile.path.startsWith(distDirectory.canonicalPath) && requestedFile.exists() && requestedFile.isFile) {
            return@get call.respondFile(requestedFile)
        }

        call.response.header(HttpHeaders.CacheControl, "no-store, no-cache, must-revalidate")
        call.respondFile(indexFile)
    }
}

private fun resolveDistDirectory(): File {
    val candidates = listOf(
        File("view/dist"),
        File("src/TicTacToe/view/dist"),
    )

    return candidates
        .map { it.absoluteFile }
        .firstOrNull { it.exists() && it.isDirectory }
        ?: candidates.first().absoluteFile
}
