package com.moviebox

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
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
                    ApiResponse<String>(success = false, error = "Missing query"),
                    status = HttpStatusCode.BadRequest
                )

                try {
                    val results = MovieBoxProvider().search(query)
                    call.respond(ApiResponse(success = true, data = results))
                } catch (e: Exception) {
                    call.respond(
                        ApiResponse<String>(success = false, error = e.message ?: "Unknown error"),
                        status = HttpStatusCode.InternalServerError
                    )
                }
            }

            get("/load") {
                val url = call.parameters["url"] ?: return@get call.respond(
                    ApiResponse<String>(success = false, error = "Missing url"),
                    status = HttpStatusCode.BadRequest
                )

                try {
                    val loadResult = MovieBoxProvider().load(url)
                    call.respond(ApiResponse(success = true, data = loadResult))
                } catch (e: Exception) {
                    call.respond(
                        ApiResponse<String>(success = false, error = e.message ?: "Unknown error"),
                        status = HttpStatusCode.InternalServerError
                    )
                }
            }

            get("/links") {
                val data = call.parameters["data"] ?: return@get call.respond(
                    ApiResponse<String>(success = false, error = "Missing data"),
                    status = HttpStatusCode.BadRequest
                )

                try {
                    val links = mutableListOf<StreamLink>()
                    MovieBoxProvider().loadLinks(data, { /* subtitles ignorés */ }, { link -> links.add(link) })
                    call.respond(ApiResponse(success = true, data = links))
                } catch (e: Exception) {
                    call.respond(
                        ApiResponse<String>(success = false, error = e.message ?: "Unknown error"),
                        status = HttpStatusCode.InternalServerError
                    )
                }
            }
        }
    }.start(wait = true)
}
