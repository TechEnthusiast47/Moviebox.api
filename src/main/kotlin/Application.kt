package com.moviebox

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json

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
                call.respondText("MovieBox API is running", ContentType.Text.Plain)
            }

            get("/search") {
                val query = call.parameters["query"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing query")
                try {
                    val results = MovieBoxProvider().search(query)
                    call.respond(results)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Error: ${e.message}")
                }
            }

            get("/load") {
                val url = call.parameters["url"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing url")
                try {
                    val load = MovieBoxProvider().load(url)
                    call.respond(load)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Error: ${e.message}")
                }
            }

            get("/links") {
                val data = call.parameters["data"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing data")
                try {
                    val links = mutableListOf<ExtractorLink>()
                    MovieBoxProvider().loadLinks(data, false, { /* subtitle ignored for now */ }, { link -> links.add(link) })
                    call.respond(links)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Error: ${e.message}")
                }
            }
        }
    }.start(wait = true)
}
