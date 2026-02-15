package com.moviebox

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.max

data class SearchResult(
    val name: String,
    val url: String,
    val type: String,
    val posterUrl: String? = null,
    val score: Int? = null
)

data class LoadResult(
    val title: String,
    val url: String,
    val type: String,
    val posterUrl: String? = null,
    val backgroundPosterUrl: String? = null,
    val plot: String? = null,
    val year: Int? = null,
    val tags: List<String> = emptyList(),
    val actors: List<String> = emptyList(),
    val score: Int? = null,
    val duration: Int? = null
)

data class StreamLink(
    val url: String,
    val name: String,
    val quality: Int? = null,
    val headers: Map<String, String> = emptyMap()
)

val client = OkHttpClient()

class MovieBoxProvider {
    val mainUrl = "https://api3.aoneroom.com"
    val name = "MovieBox"

    private fun base64Decode(encoded: String): ByteArray {
        return java.util.Base64.getDecoder().decode(encoded)
    }

    private val secretKeyDefault = base64Decode("NzZpUmwwN3MweFNOOWpxbUVXQXQ3OUVCSlp1bElRSXNWNjRGWnIyTw==")
    private val secretKeyAlt = base64Decode("WHFuMm5uTzQxL0w5Mm8xaXVYaFNMSFRiWHZZNFo1Wlo2Mm04bVNMQQ==")

    private fun md5(input: ByteArray): String {
        return MessageDigest.getInstance("MD5").digest(input)
            .joinToString("") { "%02x".format(it) }
    }

    private fun reverseString(input: String): String = input.reversed()

    private fun generateXClientToken(hardcodedTimestamp: Long? = null): String {
        val timestamp = (hardcodedTimestamp ?: System.currentTimeMillis()).toString()
        val reversed = reverseString(timestamp)
        val hash = md5(reversed.toByteArray())
        return "$timestamp,$hash"
    }

    private fun buildCanonicalString(
        method: String,
        contentType: String,
        accept: String,
        url: String,
        body: String
    ): String {
        val canonical = "$method\n$accept\n$contentType\n$url\n$body"
        return canonical.trimEnd()
    }

    private fun generateXTrSignature(method: String, accept: String, contentType: String, url: String, body: String): String {
        val canonical = buildCanonicalString(method, contentType, accept, url, body)
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secretKeyDefault, "HmacSHA256"))
        val hash = mac.doFinal(canonical.toByteArray())
        return java.util.Base64.getEncoder().encodeToString(hash)
    }

    suspend fun search(query: String): List<SearchResult> {
        val url = "$mainUrl/wefeed-mobile-bff/subject-api/search/v2"
        val jsonBody = """{"page": 1, "perPage": 10, "keyword": "$query"}"""
        val xClientToken = generateXClientToken()
        val xTrSignature = generateXTrSignature("POST", "application/json", "application/json; charset=utf-8", url, jsonBody)
        val headers = mapOf(
            "user-agent" to "com.community.mbox.in/50020042 (Linux; U; Android 16; en_IN; sdk_gphone64_x86_64; Build/BP22.250325.006; Cronet/133.0.6876.3)",
            "accept" to "application/json",
            "content-type" to "application/json",
            "connection" to "keep-alive",
            "x-client-token" to xClientToken,
            "x-tr-signature" to xTrSignature,
            "x-client-info" to """{"package_name":"com.community.mbox.in","version_name":"3.0.03.0529.03","version_code":50020042,"os":"android","os_version":"16","device_id":"da2b99c821e6ea023e4be55b54d5f7d8","install_store":"ps","gaid":"d7578036d13336cc","brand":"google","model":"sdk_gphone64_x86_64","system_language":"en","net":"NETWORK_WIFI","region":"IN","timezone":"Asia/Calcutta","sp_code":""}""",
            "x-client-status" to "0"
        )
        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).apply {
            headers.forEach { (k, v) -> addHeader(k, v) }
        }.build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: return emptyList()

        val mapper = jacksonObjectMapper()
        val root = mapper.readValue<JsonNode>(responseBody)
        val items = root.at("/data/results")?.getElements() ?: return emptyList()

        return buildList {
            while (items.hasNext()) {
                val subject = items.next()
                val title = subject ?.asText() ?: continue
                val id = subject ?.asText() ?: continue
                val cover = subject ?.get("url")?.asText()
                val type = when (subject ?.asInt()) {
                    1 -> "movie"
                    2 -> "tv"
                    else -> "movie"
                }
                add(SearchResult(title, id, type, cover))
            }
        }
    }
}
