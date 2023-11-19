package com.nvvi9.ytstream.utils

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
private val Json = Json(builderAction = {
    explicitNulls = false
    ignoreUnknownKeys = true
})