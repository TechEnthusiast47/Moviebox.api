package com.moviebox

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.net.URLEncoder
import java.util.Base64

data class SearchResult(
    val name: String,
    val url: String,
    val type: String,
    val posterUrl: String? = null,
    val score: Int? = null
)

val client = OkHttpClient()

class MovieBoxProvider {
    val mainUrl = "https://api3.aoneroom.com"
    val name = "MovieBox"

    private fun base64Decode(encoded: String): ByteArray {
        return Base64.getDecoder().decode(encoded)
    }

    suspend fun search(query: String): List<SearchResult> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "$mainUrl/wefeed-mobile-bff/subject-api/search/v2?keyword=$encodedQuery&page=1&perPage=10"

        val request = Request.Builder()
            .url(url)
            .get()
            .header("User-Agent", "com.community.mbox.in/50020042 (Linux; U; Android 16; en_IN; sdk_gphone64_x86_64; Build/BP22.250325.006; Cronet/133.0.6876.3)")
            .header("Accept", "application/json")
            .header("Connection", "keep-alive")
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: return emptyList()

        val mapper = jacksonObjectMapper()
        val root = mapper.readTree(responseBody)
        val results = root["data"]?.get("results") ?: return emptyList()

        return results.map { node ->
            SearchResult(
                name = node["title"]?.asText() ?: "No title",
                url = node["subjectId"]?.asText() ?: "",
                type = if (node["subjectType"]?.asInt() == 2) "tv" else "movie",
                posterUrl = node["cover"]?.get("url")?.asText()
            )
        }
    }
}
