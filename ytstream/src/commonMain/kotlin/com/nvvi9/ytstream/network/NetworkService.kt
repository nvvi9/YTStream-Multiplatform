package com.nvvi9.ytstream.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

object NetworkService {

    @OptIn(ExperimentalSerializationApi::class)
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            })
        }

        install(UserAgent) {
            agent =
                "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/97.0.4692.98 Safari/537.36"
        }
    }

    suspend fun getVideoPage(id: String): Result<String> = runCatching {
        httpClient.get("https://www.youtube.com/watch?v=$id").bodyAsText()
    }

    suspend fun getJsFile(jsPath: String): Result<String> = runCatching {
        httpClient.get("https://www.youtube.com$jsPath").bodyAsText()
    }
}