package com.nvvi9.ytstream

import android.content.Context
import com.nvvi9.ytstream.extractors.InitialPlayerResponseExtractor
import com.nvvi9.ytstream.extractors.StreamExtractor
import com.nvvi9.ytstream.js.JsExecutor
import com.nvvi9.ytstream.model.VideoData
import com.nvvi9.ytstream.model.VideoDetails
import com.nvvi9.ytstream.model.toVideoDetails
import com.nvvi9.ytstream.network.NetworkService
import com.nvvi9.ytstream.utils.flatMap

actual class YTStream(private val context: Context) {

    private val jsExecutor = JsExecutor(context)
    private val streamExtractor = StreamExtractor(jsExecutor)
    private val initialPlayerResponseExtractor = InitialPlayerResponseExtractor()

    actual suspend fun extractVideoData(id: String): Result<VideoData> =
        NetworkService.getVideoPage(id).flatMap { pageHtml ->
            initialPlayerResponseExtractor.extractInitialPlayerResponse(pageHtml)
                .map { initialPlayerResponse ->
                    val streamingData = initialPlayerResponse.streamingData
                    val videoDetails = initialPlayerResponse.toVideoDetails()
                    val formats = (streamingData.formats + streamingData.adaptiveFormats)
                        .filter { it.type != "FORMAT_STREAM_TYPE_OTF" }

                    val streams = streamExtractor.extractStreams(pageHtml, formats)
                    VideoData(videoDetails, streams)
                }
        }

    actual suspend fun extractVideoDetails(id: String): Result<VideoDetails> =
        initialPlayerResponseExtractor.extractInitialPlayerResponse(id)
            .mapCatching { it.toVideoDetails() }
}