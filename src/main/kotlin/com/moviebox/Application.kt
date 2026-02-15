package com.moviebox

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null
)

fun main() {
    embeddedServer(Netty, port = System.getenv("PORT")?.toInt() ?: 8080) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }

        routing {
            get("/") {
                call.respondText("MovieBox API is running ✅", ContentType.Text.Plain)
            }

            get("/search") {
                val query = call.parameters["query"] ?: return@get call.respond(
                    ApiResponse<String>(false, null, "Missing query"),
                    status = HttpStatusCode.BadRequest
                )

                try {
                    val results = MovieBoxProvider().search(query)
                    call.respond(ApiResponse(true, results, null))
                } catch (e: Exception) {
                    call.respond(
                        ApiResponse<String>(false, null, e.message ?: "Error"),
                        status = HttpStatusCode.InternalServerError
                    )
                }
            }
        }
    }.start(wait = true)
}
