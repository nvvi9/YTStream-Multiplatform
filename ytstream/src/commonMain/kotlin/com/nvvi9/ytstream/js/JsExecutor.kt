package com.nvvi9.ytstream.js

expect class JsExecutor {
    suspend fun executeScript(script: String): Result<String>
}