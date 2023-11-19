package com.nvvi9.ytstream.utils

import io.ktor.http.decodeURLPart
import io.ktor.utils.io.charsets.Charsets

fun String.decodeUrl() = this.decodeURLPart(charset = Charsets.UTF_8)