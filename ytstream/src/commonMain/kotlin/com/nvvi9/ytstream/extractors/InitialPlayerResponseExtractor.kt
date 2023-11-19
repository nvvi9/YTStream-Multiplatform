package com.nvvi9.ytstream.extractors

import com.nvvi9.ytstream.model.youtube.InitialPlayerResponse
import com.nvvi9.ytstream.network.NetworkService
import com.nvvi9.ytstream.utils.patternPlayerResponse
import kotlinx.serialization.json.Json

class InitialPlayerResponseExtractor {

    suspend fun extractInitialPlayerResponse(id: String): Result<InitialPlayerResponse> =
        NetworkService.getVideoPage(id).mapCatching { pageHtml ->
            val playerResponse = patternPlayerResponse.toRegex().find(pageHtml)?.groupValues?.get(1)
                ?: throw IllegalStateException()

            Json.decodeFromString(playerResponse)
        }

    fun extractInitialPlayerResponseFromHtml(pageHtml: String): Result<InitialPlayerResponse> =
        runCatching {
            val playerResponse = patternPlayerResponse.toRegex().find(pageHtml)?.groupValues?.get(1)
                ?: throw IllegalStateException()

            Json.decodeFromString(playerResponse)
        }
}