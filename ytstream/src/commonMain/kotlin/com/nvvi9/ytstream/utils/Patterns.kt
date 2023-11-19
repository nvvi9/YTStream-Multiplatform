package com.nvvi9.ytstream.utils

const val patternPlayerResponse = "var ytInitialPlayerResponse\\s*=\\s*(\\{.+?\\})\\s*;"
const val patternSigEncUrl = "url=(.+?)(\\u0026|$)"
const val patternSignature = "s=(.+?)(\\u0026|$)"
const val patternDecryptionJsFile = "\\\\/s\\\\/player\\\\/([^\"]+?)\\.js"
const val patternDecryptionJsFileWithoutSlash = "/s/player/([^\"]+?).js"
const val patternSignatureDecFunction =
    "(?:\\b|[^a-zA-Z0-9$])([a-zA-Z0-9$]{1,4})\\s*=\\s*function\\(\\s*a\\s*\\)\\s*\\{\\s*a\\s*=\\s*a\\.split\\(\\s*\"\"\\s*\\)"
const val patternVariableFunction =
    "([{; =])([a-zA-Z$][a-zA-Z0-9$]{0,2})\\.([a-zA-Z$][a-zA-Z0-9$]{0,2})\\("
const val patternFunction = "([{; =])([a-zA-Z\$_][a-zA-Z0-9$]{0,2})\\("