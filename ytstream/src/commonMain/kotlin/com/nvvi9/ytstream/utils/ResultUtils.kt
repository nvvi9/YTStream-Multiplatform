package com.nvvi9.ytstream.utils

@Suppress("UNCHECKED_CAST")
internal inline fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> =
    fold({ transform(it) }, { this as Result<R> })