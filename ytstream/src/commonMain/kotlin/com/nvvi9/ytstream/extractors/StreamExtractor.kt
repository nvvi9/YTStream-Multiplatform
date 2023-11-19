package com.nvvi9.ytstream.extractors

import com.nvvi9.ytstream.js.JsExecutor
import com.nvvi9.ytstream.model.signature.EncodedSignature
import com.nvvi9.ytstream.model.streams.Stream
import com.nvvi9.ytstream.model.youtube.InitialPlayerResponse
import com.nvvi9.ytstream.network.NetworkService
import com.nvvi9.ytstream.utils.patternDecryptionJsFile
import com.nvvi9.ytstream.utils.patternDecryptionJsFileWithoutSlash
import com.nvvi9.ytstream.utils.patternFunction
import com.nvvi9.ytstream.utils.patternSigEncUrl
import com.nvvi9.ytstream.utils.patternSignature
import com.nvvi9.ytstream.utils.patternSignatureDecFunction
import com.nvvi9.ytstream.utils.patternVariableFunction

class StreamExtractor(private val jsExecutor: JsExecutor) {

    suspend fun extractStreams(
        pageHtml: String,
        formats: List<InitialPlayerResponse.StreamingData.Format>
    ): List<Stream> = extractUnencodedStreams(formats) + extractEncodedStreams(pageHtml, formats)

    private fun extractUnencodedStreams(formats: List<InitialPlayerResponse.StreamingData.Format>) =
        formats.mapNotNull { format ->
            val itag = format.itag
            format.url?.replace("\\u0026", "&")?.let { url ->
                Stream.ITAG_MAP[itag]?.let { Stream(url, it) }
            }
        }

    private suspend fun extractEncodedStreams(
        pageHtml: String,
        formats: List<InitialPlayerResponse.StreamingData.Format>
    ): List<Stream> = getEncodedSignatures(formats)
        .takeIf { it.isNotEmpty() }
        ?.let { encodedSignatures ->
            val matcher = patternDecryptionJsFile.toRegex().find(pageHtml)
                ?: patternDecryptionJsFileWithoutSlash.toRegex().find(pageHtml)

            val decipherJsFileName = matcher?.groupValues?.get(0)?.replace("\\/", "/")
                ?: return emptyList()

            val signatures = decipherSignature(encodedSignatures, decipherJsFileName)
                .map { it.split("\n") }
                .getOrDefault(emptyList())

            encodedSignatures.zip(signatures).map { (encSignature, sig) ->
                Stream("${encSignature.url}&sig=$sig", encSignature.streamDetails)
            }
        } ?: emptyList()

    private fun getEncodedSignatures(formats: List<InitialPlayerResponse.StreamingData.Format>) =
        formats.mapNotNull { format ->
            format.signatureCipher?.let { signatureCipher ->
                val url = patternSigEncUrl.toRegex().find(signatureCipher)?.groups?.get(1)?.value
                    ?: return@mapNotNull null
                val signature =
                    patternSignature.toRegex().find(signatureCipher)?.groups?.get(1)?.value
                        ?: return@mapNotNull null

                Stream.ITAG_MAP[format.itag]?.let { EncodedSignature(url, signature, it) }
            }
        }

    private suspend fun decipherSignature(
        encSignatures: List<EncodedSignature>,
        decipherJsFileName: String
    ): Result<String> {
        val javaScriptFile =
            NetworkService.getJsFile(decipherJsFileName).getOrNull()
                ?: throw IllegalStateException()

        val matcher = patternSignatureDecFunction.toRegex().find(javaScriptFile)
            ?: throw IllegalStateException()
        val decipherFunctionName = matcher.groupValues[1]
        val patternMainVariable = Regex(
            "(var |\\s|,|;)"
                    + decipherFunctionName.replace("$", "\\$")
                    + "(=function\\((.{1,3})\\)\\{)"
        )

        var mainDecipherFunct: String

        var mainDecipherFunctionMatcher = patternMainVariable.find(javaScriptFile)
        if (mainDecipherFunctionMatcher != null) {
            mainDecipherFunct =
                "var $decipherFunctionName${mainDecipherFunctionMatcher.groupValues[2]}"
        } else {
            val patternMainFunction = Regex(
                "function "
                        + decipherFunctionName.replace("$", "\\$")
                        + "(\\((.{1,3})\\)\\{)"
            )

            mainDecipherFunctionMatcher =
                patternMainFunction.find(javaScriptFile) ?: throw IllegalStateException()

            mainDecipherFunct =
                "function ${decipherFunctionName}${mainDecipherFunctionMatcher.groupValues[2]}"
        }

        var startIndex = mainDecipherFunctionMatcher.range.last + 1

        var i = startIndex
        var braces = 1

        while (i < javaScriptFile.length) {
            if (braces == 0 && startIndex + 5 < i) {
                mainDecipherFunct += "${javaScriptFile.substring(startIndex, i)};"
                break
            }
            if (javaScriptFile[i] == '{') {
                braces++
            } else if (javaScriptFile[i] == '}') {
                braces--
            }
            i++
        }

        var decipherFunctions = mainDecipherFunct

        val variableFunctionMatcher = patternVariableFunction.toRegex().findAll(mainDecipherFunct)
        for (variableFunctionMatch in variableFunctionMatcher) {
            val variableDef = "var ${variableFunctionMatch.groupValues[1]}={"
            if (variableDef in decipherFunctions) {
                continue
            }

            startIndex = javaScriptFile.indexOf(variableDef) + variableDef.length
            i = startIndex
            braces = 1
            while (i < javaScriptFile.length) {
                if (braces == 0) {
                    decipherFunctions += "$variableDef${
                        javaScriptFile.substring(
                            startIndex,
                            i
                        )
                    };"
                    break
                }
                if (javaScriptFile[i] == '{') {
                    braces++
                } else if (javaScriptFile[i] == '}') {
                    braces--
                }
                i++
            }
        }

        val functionMatcher = patternFunction.toRegex().findAll(mainDecipherFunct)
        for (functionMatch in functionMatcher) {
            val functionDef = "function ${functionMatch.groupValues[1]}("
            if (functionDef in decipherFunctions) {
                continue
            }

            startIndex = javaScriptFile.indexOf(functionDef) + functionDef.length
            i = 0
            braces = 0
            while (i < javaScriptFile.length) {
                if (braces == 0 && startIndex + 5 < i) {
                    decipherFunctions += "$functionDef${
                        javaScriptFile.substring(startIndex, i)
                    };"
                    break
                }
                if (javaScriptFile[i] == '{') {
                    braces++
                } else if (javaScriptFile[i] == '}') {
                    braces--
                }
                i++
            }
        }

        return decipherEncodedSignatures(
            encSignatures.map { it.signature },
            decipherFunctions,
            decipherFunctionName
        )
    }

    private suspend fun decipherEncodedSignatures(
        encSignatures: List<String>,
        decipherFunctions: String,
        decipherFunctionName: String
    ): Result<String> {
        val script =
            "$decipherFunctions function decipher(){return " + encSignatures.foldIndexed("") { index, acc, s ->
                acc + decipherFunctionName + "('" + s + if (index < encSignatures.size - 1) "')+\"\\n\"+" else "')"
            } + "};decipher();"

        return jsExecutor.executeScript(script)
    }
}