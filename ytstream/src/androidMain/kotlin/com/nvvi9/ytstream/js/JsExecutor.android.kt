package com.nvvi9.ytstream.js

import android.content.Context
import androidx.javascriptengine.JavaScriptSandbox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

actual class JsExecutor(private val context: Context) {

    private val mutex = Mutex()

    actual suspend fun executeScript(script: String): Result<String> = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                val jsSandbox = JavaScriptSandbox.createConnectedInstanceAsync(context).get()
                val jsIsolate = jsSandbox.createIsolate()
                jsIsolate.evaluateJavaScriptAsync(script).get().also {
                    jsSandbox.close()
                }
            }
        }
    }
}