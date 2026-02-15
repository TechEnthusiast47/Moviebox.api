package com.moviebox

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.OkHttpClient
import okhttp3.Request

class MovieBoxProvider {
    val mainUrl = "https://api3.aoneroom.com"
    val name = "MovieBox"

    suspend fun search(query: String): List<SearchResult> {
        val url = "$mainUrl/wefeed-mobile-bff/subject-api/search/v2?query=$query"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return emptyList()
        
        // Parse JSON simplement (ajuste selon ta structure réelle)
        val mapper = jacksonObjectMapper()
        val root = mapper.readTree(body)
        val results = root["data"]?.get("results") ?: return emptyList()
        
        return results.map { node ->
            SearchResult(
                name = node["title"]?.asText() ?: "No title",
                url = node["subjectId"]?.asText() ?: "",
                type = "movie",
                posterUrl = node["cover"]?.get("url")?.asText()
            )
        }
    }
}
