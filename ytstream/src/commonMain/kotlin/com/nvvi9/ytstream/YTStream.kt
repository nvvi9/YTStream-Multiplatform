package com.nvvi9.ytstream

import com.nvvi9.ytstream.model.VideoData
import com.nvvi9.ytstream.model.VideoDetails

expect class YTStream {
    suspend fun extractVideoData(id: String): Result<VideoData>
    suspend fun extractVideoDetails(id: String): Result<VideoDetails>
}