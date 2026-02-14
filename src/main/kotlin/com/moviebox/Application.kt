package com.moviebox

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
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
                call.respondText("MovieBox API is running ✅")
            }

            get("/search") {
                val query = call.parameters["query"] ?: return@get call.respondText("Missing query", status = HttpStatusCode.BadRequest)
                try {
                    val results = MovieBoxProvider().search(query)
                    call.respond(results)
                } catch (e: Exception) {
                    call.respondText("Error: ${e.message}", status = HttpStatusCode.InternalServerError)
                }
            }

            get("/load") {
                val url = call.parameters["url"] ?: return@get call.respondText("Missing url", status = HttpStatusCode.BadRequest)
                try {
                    val loadResponse = MovieBoxProvider().load(url)
                    call.respond(loadResponse)
                } catch (e: Exception) {
                    call.respondText("Error: ${e.message}", status = HttpStatusCode.InternalServerError)
                }
            }

            get("/links") {
                val data = call.parameters["data"] ?: return@get call.respondText("Missing data", status = HttpStatusCode.BadRequest)
                try {
                    val links = mutableListOf<ExtractorLink>()
                    MovieBoxProvider().loadLinks(data, false, { subtitleCallback -> }, { link -> links.add(link) })
                    call.respond(links)
                } catch (e: Exception) {
                    call.respondText("Error: ${e.message}", status = HttpStatusCode.InternalServerError)
                }
            }
        }
    }.start(wait = true)
}
