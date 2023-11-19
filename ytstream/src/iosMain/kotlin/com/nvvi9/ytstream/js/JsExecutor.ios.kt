package com.nvvi9.ytstream.js

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.UIKit.UIWebView

actual class JsExecutor {

    actual suspend fun executeScript(script: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            UIWebView().stringByEvaluatingJavaScriptFromString(script)
                ?: throw IllegalStateException()
        }
    }
}