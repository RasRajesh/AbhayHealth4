package com.example.abhayhealth.feature.records

import com.example.abhayhealth.core.security.SecurityAudit
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

/**
 * Ultra-fast, zero-bitmap digital PDF text extractor.
 *
 * Extracts text directly from digital PDF content streams (FlateDecode / raw)
 * without rendering bitmaps or invoking ML Kit. This eliminates native memory pressure,
 * provides sub-50ms extraction for digital lab reports & hospital packages, and prevents
 * OutOfMemory / SIGABRT crashes on memory-constrained devices.
 */
object PdfTextExtractor {

    private const val MAX_PDF_BYTES_FOR_EXTRACTION = 25 * 1024 * 1024 // 25 MiB
    private const val MAX_CONTENT_STREAMS = 150
    private const val MAX_STREAM_BYTES = 2 * 1024 * 1024 // 2 MiB per stream
    private const val MAX_TOTAL_EXTRACTED_CHARS = 250_000

    fun extractText(file: File): String? {
        if (!file.exists() || !file.canRead() || file.length() == 0L) return null
        if (file.length() > MAX_PDF_BYTES_FOR_EXTRACTION) return null

        return try {
            val bytes = file.readBytes()
            val text = extractTextFromPdfBytes(bytes)
            if (text.isNotBlank() && text.length >= 30) {
                text
            } else {
                null
            }
        } catch (t: Throwable) {
            SecurityAudit.logError("PdfTextExtractor", "Direct PDF stream extraction failed", t)
            null
        }
    }

    fun extractTextFromPdfBytes(bytes: ByteArray): String {
        val streams = extractContentStreams(bytes)
        if (streams.isEmpty()) return ""

        val resultBuilder = StringBuilder()

        for (stream in streams) {
            if (resultBuilder.length >= MAX_TOTAL_EXTRACTED_CHARS) break

            val streamText = parsePdfContentStream(stream)
            if (streamText.isNotBlank()) {
                if (resultBuilder.isNotEmpty() && !resultBuilder.endsWith("\n")) {
                    resultBuilder.append("\n")
                }
                resultBuilder.append(streamText)
            }
        }

        return normalizeExtractedText(resultBuilder.toString())
    }

    private fun extractContentStreams(bytes: ByteArray): List<ByteArray> {
        val streams = mutableListOf<ByteArray>()
        var index = 0
        val streamKeyword = "stream".toByteArray(Charsets.US_ASCII)
        val endstreamKeyword = "endstream".toByteArray(Charsets.US_ASCII)
        val objKeyword = "obj".toByteArray(Charsets.US_ASCII)
        val dictStartKeyword = "<<".toByteArray(Charsets.US_ASCII)

        while (index < bytes.size && streams.size < MAX_CONTENT_STREAMS) {
            val streamStart = indexOfBytes(bytes, streamKeyword, index)
            if (streamStart == -1) break

            // Inspect ONLY the dictionary header of this specific object (after the nearest 'obj' or '<<')
            val objIndex = lastIndexOfBytes(bytes, objKeyword, streamStart, maxLookback = 600)
            val dictIndex = lastIndexOfBytes(bytes, dictStartKeyword, streamStart, maxLookback = 600)
            val startOfDict = maxOf(0, maxOf(objIndex, dictIndex))
            val dictHeader = if (streamStart > startOfDict) {
                String(bytes, startOfDict, streamStart - startOfDict, Charsets.US_ASCII)
            } else ""

            if (isBinaryDictionary(dictHeader)) {
                val nextEnd = indexOfBytes(bytes, endstreamKeyword, streamStart)
                if (nextEnd == -1) break
                index = nextEnd + endstreamKeyword.size
                continue
            }

            // Skip 'stream' and following newline (\r\n or \n)
            var contentStart = streamStart + streamKeyword.size
            if (contentStart < bytes.size && bytes[contentStart] == '\r'.code.toByte()) contentStart++
            if (contentStart < bytes.size && bytes[contentStart] == '\n'.code.toByte()) contentStart++

            val endStream = indexOfBytes(bytes, endstreamKeyword, contentStart)
            if (endStream == -1) break

            var contentEnd = endStream
            // Backtrack whitespace before endstream
            while (contentEnd > contentStart && (bytes[contentEnd - 1] == '\n'.code.toByte() || bytes[contentEnd - 1] == '\r'.code.toByte())) {
                contentEnd--
            }

            if (contentEnd > contentStart) {
                val streamLength = contentEnd - contentStart
                if (streamLength in 1..MAX_STREAM_BYTES) {
                    val rawStream = ByteArray(streamLength)
                    System.arraycopy(bytes, contentStart, rawStream, 0, streamLength)

                    // Try inflating if FlateDecode
                    val inflated = tryInflate(rawStream)
                    val targetStream = inflated ?: rawStream

                    // Only retain streams that actually contain PDF text operators (BT)
                    if (containsTextOperators(targetStream)) {
                        streams.add(targetStream)
                    }
                }
            }

            index = endStream + endstreamKeyword.size
        }

        return streams
    }

    private fun isBinaryDictionary(dict: String): Boolean {
        if (dict.isBlank()) return false
        return dict.contains("/Subtype/Image", ignoreCase = true) ||
                dict.contains("/Subtype /Image", ignoreCase = true) ||
                dict.contains("/Filter/DCTDecode", ignoreCase = true) ||
                dict.contains("/Filter /DCTDecode", ignoreCase = true) ||
                dict.contains("/Filter/JBIG2Decode", ignoreCase = true) ||
                dict.contains("/Filter /JBIG2Decode", ignoreCase = true) ||
                dict.contains("/Filter/JPXDecode", ignoreCase = true) ||
                dict.contains("/Filter /JPXDecode", ignoreCase = true) ||
                dict.contains("/Type/Font", ignoreCase = true) ||
                dict.contains("/Type /Font", ignoreCase = true) ||
                dict.contains("/Subtype/Type1C", ignoreCase = true) ||
                dict.contains("/Subtype /Type1C", ignoreCase = true) ||
                dict.contains("/Subtype/CIDFontType0C", ignoreCase = true) ||
                dict.contains("/Subtype /CIDFontType0C", ignoreCase = true)
    }

    private fun containsTextOperators(streamBytes: ByteArray): Boolean {
        // Fast scan for 'BT' preceded or followed by whitespace
        for (i in 0 until streamBytes.size - 2) {
            if (streamBytes[i] == 'B'.code.toByte() && streamBytes[i + 1] == 'T'.code.toByte()) {
                val next = streamBytes[i + 2].toInt().toChar()
                if (next.isWhitespace() || next == '\r' || next == '\n') {
                    return true
                }
            }
        }
        return false
    }

    private fun tryInflate(data: ByteArray): ByteArray? {
        // Try standard zlib inflater
        try {
            val inflater = Inflater(false)
            inflater.setInput(data)
            val buffer = ByteArray(8192)
            val out = ByteArrayOutputStream()
            while (!inflater.finished() && out.size() < MAX_STREAM_BYTES) {
                val count = inflater.inflate(buffer)
                if (count <= 0) {
                    if (inflater.needsInput() || inflater.needsDictionary()) break
                }
                out.write(buffer, 0, count)
            }
            inflater.end()
            val result = out.toByteArray()
            if (result.isNotEmpty()) return result
        } catch (_: Throwable) {
        }

        // Try raw deflate (no zlib header)
        try {
            val inflater = Inflater(true)
            inflater.setInput(data)
            val buffer = ByteArray(8192)
            val out = ByteArrayOutputStream()
            while (!inflater.finished() && out.size() < MAX_STREAM_BYTES) {
                val count = inflater.inflate(buffer)
                if (count <= 0) {
                    if (inflater.needsInput() || inflater.needsDictionary()) break
                }
                out.write(buffer, 0, count)
            }
            inflater.end()
            val result = out.toByteArray()
            if (result.isNotEmpty()) return result
        } catch (_: Throwable) {
        }

        // Try InflaterInputStream
        try {
            InflaterInputStream(ByteArrayInputStream(data)).use { input ->
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1 && out.size() < MAX_STREAM_BYTES) {
                    out.write(buffer, 0, read)
                }
                val result = out.toByteArray()
                if (result.isNotEmpty()) return result
            }
        } catch (_: Throwable) {
        }

        return null
    }

    private fun parsePdfContentStream(streamBytes: ByteArray): String {
        val streamText = String(streamBytes, Charsets.ISO_8859_1)
        val sb = StringBuilder()
        var pos = 0
        val len = streamText.length

        while (pos < len) {
            // Find the start of the next text block (BT)
            val btIndex = findOperator(streamText, "BT", pos)
            if (btIndex == -1) break

            val etIndex = findOperator(streamText, "ET", btIndex + 2)
            val textBlockEnd = if (etIndex != -1) etIndex else len

            var blockPos = btIndex + 2
            while (blockPos < textBlockEnd) {
                val c = streamText[blockPos]

                // 1. Array with TJ operator: [ (str1) -20 (str2) ] TJ
                if (c == '[') {
                    val arrayEnd = streamText.indexOf(']', blockPos + 1)
                    if (arrayEnd != -1 && arrayEnd < textBlockEnd) {
                        val arrayContent = streamText.substring(blockPos + 1, arrayEnd)
                        val arrayText = parseTjArray(arrayContent)
                        if (arrayText.isNotBlank()) {
                            appendWithSpacing(sb, arrayText)
                        }
                        blockPos = arrayEnd + 1
                        continue
                    }
                }

                // 2. String literal: ( ... )
                if (c == '(') {
                    val start = blockPos + 1
                    var depth = 1
                    var i = start
                    var escaped = false

                    while (i < textBlockEnd && depth > 0) {
                        val curr = streamText[i]
                        if (escaped) {
                            escaped = false
                        } else if (curr == '\\') {
                            escaped = true
                        } else if (curr == '(') {
                            depth++
                        } else if (curr == ')') {
                            depth--
                        }
                        i++
                    }

                    if (depth == 0) {
                        val rawStr = streamText.substring(start, i - 1)
                        val unescaped = unescapePdfString(rawStr)
                        if (unescaped.isNotBlank()) {
                            appendWithSpacing(sb, unescaped)
                        }
                        blockPos = i
                        continue
                    } else {
                        // Unmatched parenthesis, advance by 1 character past the '('
                        blockPos = start
                        continue
                    }
                }

                // 3. Hex string: < ... > (excluding dictionary <<)
                if (c == '<' && blockPos + 1 < textBlockEnd && streamText[blockPos + 1] != '<') {
                    val end = streamText.indexOf('>', blockPos + 1)
                    if (end != -1 && end < textBlockEnd && end - blockPos < 1000) {
                        val hex = streamText.substring(blockPos + 1, end).trim()
                        val decoded = decodeHexPdfString(hex)
                        if (decoded.isNotBlank()) {
                            appendWithSpacing(sb, decoded)
                        }
                        blockPos = end + 1
                        continue
                    }
                }

                // 4. Line breaking operators: T*, ', ", Td, TD
                if (c == 'T' && blockPos + 1 < textBlockEnd) {
                    val next = streamText[blockPos + 1]
                    if (next == '*' || next == 'D' || next == 'd') {
                        if (sb.isNotEmpty() && !sb.endsWith('\n')) {
                            sb.append("\n")
                        }
                    }
                } else if (c == '\'' || c == '"') {
                    if (sb.isNotEmpty() && !sb.endsWith('\n')) {
                        sb.append("\n")
                    }
                }

                blockPos++
            }

            // End of text block (ET), ensure line separation
            if (sb.isNotEmpty() && !sb.endsWith('\n')) {
                sb.append("\n")
            }

            pos = if (etIndex != -1) etIndex + 2 else len
        }

        return sb.toString().trim()
    }

    private fun findOperator(text: String, op: String, fromIndex: Int): Int {
        var idx = fromIndex
        while (idx < text.length) {
            val found = text.indexOf(op, idx)
            if (found == -1) return -1

            // Operator must be bounded by whitespace or start/end of line
            val prevOk = found == 0 || text[found - 1].isWhitespace() || text[found - 1] == '\r' || text[found - 1] == '\n'
            val nextOk = (found + op.length >= text.length) || text[found + op.length].isWhitespace() || text[found + op.length] == '\r' || text[found + op.length] == '\n'

            if (prevOk && nextOk) {
                return found
            }
            idx = found + 1
        }
        return -1
    }

    private fun parseTjArray(arrayContent: String): String {
        val result = StringBuilder()
        var pos = 0
        val len = arrayContent.length

        while (pos < len) {
            val c = arrayContent[pos]

            if (c == '(') {
                val start = pos + 1
                var depth = 1
                var i = start
                var escaped = false

                while (i < len && depth > 0) {
                    val curr = arrayContent[i]
                    if (escaped) {
                        escaped = false
                    } else if (curr == '\\') {
                        escaped = true
                    } else if (curr == '(') {
                        depth++
                    } else if (curr == ')') {
                        depth--
                    }
                    i++
                }

                if (depth == 0) {
                    val rawStr = arrayContent.substring(start, i - 1)
                    val unescaped = unescapePdfString(rawStr)
                    result.append(unescaped)
                    pos = i
                    continue
                } else {
                    pos = start
                    continue
                }
            } else if (c == '<' && pos + 1 < len && arrayContent[pos + 1] != '<') {
                val end = arrayContent.indexOf('>', pos + 1)
                if (end != -1) {
                    val hex = arrayContent.substring(pos + 1, end).trim()
                    val decoded = decodeHexPdfString(hex)
                    result.append(decoded)
                    pos = end + 1
                    continue
                }
            } else if (c == '-' || c in '0'..'9') {
                // Parse number for kerning / space detection
                var numEnd = pos + 1
                while (numEnd < len && (arrayContent[numEnd] in '0'..'9' || arrayContent[numEnd] == '.')) {
                    numEnd++
                }
                val numStr = arrayContent.substring(pos, numEnd)
                val numVal = numStr.toFloatOrNull() ?: 0f
                // In PDF fonts, a large negative displacement (e.g. < -180) denotes a word space.
                if (numVal < -180f && result.isNotEmpty() && !result.endsWith(" ")) {
                    result.append(" ")
                }
                pos = numEnd
                continue
            }

            pos++
        }

        return result.toString()
    }

    private fun appendWithSpacing(sb: StringBuilder, text: String) {
        if (text.isBlank()) return
        if (sb.isNotEmpty() && !sb.endsWith(" ") && !sb.endsWith("\n") &&
            !text.startsWith(" ") && !text.startsWith(",") &&
            !text.startsWith(":") && !text.startsWith(")") &&
            !text.startsWith("-") && !text.startsWith("/") &&
            !text.startsWith("%")
        ) {
            sb.append(" ")
        }
        sb.append(text)
    }

    private fun normalizeExtractedText(text: String): String {
        return text
            .replace(Regex("""[ \t]+"""), " ")
            .replace(Regex("""\n{3,}"""), "\n\n")
            .trim()
    }

    private fun unescapePdfString(input: String): String {
        val out = StringBuilder()
        var i = 0
        val len = input.length

        while (i < len) {
            val c = input[i]
            if (c == '\\' && i + 1 < len) {
                val next = input[i + 1]
                when (next) {
                    'n' -> { out.append('\n'); i += 2 }
                    'r' -> { out.append('\r'); i += 2 }
                    't' -> { out.append('\t'); i += 2 }
                    'b' -> { out.append('\b'); i += 2 }
                    'f' -> { out.append('\u000C'); i += 2 }
                    '(', ')', '\\' -> { out.append(next); i += 2 }
                    in '0'..'7' -> {
                        // Octal sequence \ddd (up to 3 octal digits)
                        var octal = ""
                        var j = i + 1
                        while (j < len && j - (i + 1) < 3 && input[j] in '0'..'7') {
                            octal += input[j]
                            j++
                        }
                        val code = octal.toIntOrNull(8) ?: 32
                        out.append(code.toChar())
                        i = j
                    }
                    else -> {
                        out.append(next)
                        i += 2
                    }
                }
            } else {
                out.append(c)
                i++
            }
        }

        return out.toString()
    }

    private fun decodeHexPdfString(hex: String): String {
        val clean = hex.replace(Regex("""\s+"""), "")
        if (clean.length < 2) return ""

        val bytes = ByteArray(clean.length / 2)
        var i = 0
        while (i + 1 < clean.length) {
            val byteVal = clean.substring(i, i + 2).toIntOrNull(16) ?: break
            bytes[i / 2] = byteVal.toByte()
            i += 2
        }

        // Check for UTF-16BE (starts with FEFF or alternating 0x00 bytes)
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
        }
        if (bytes.size >= 4 && bytes[0] == 0.toByte() && bytes[2] == 0.toByte()) {
            return String(bytes, Charsets.UTF_16BE)
        }

        return String(bytes, Charsets.ISO_8859_1).filter {
            it.isLetterOrDigit() || it.isWhitespace() || it in ".,-/:%()><[]#+*="
        }
    }

    private fun indexOfBytes(source: ByteArray, target: ByteArray, fromIndex: Int = 0): Int {
        if (target.isEmpty() || fromIndex >= source.size) return -1
        val max = source.size - target.size
        for (i in fromIndex..max) {
            var match = true
            for (j in target.indices) {
                if (source[i + j] != target[j]) {
                    match = false
                    break
                }
            }
            if (match) return i
        }
        return -1
    }

    private fun lastIndexOfBytes(source: ByteArray, target: ByteArray, beforeIndex: Int, maxLookback: Int = 600): Int {
        if (target.isEmpty() || beforeIndex <= 0) return -1
        val start = maxOf(0, beforeIndex - maxLookback)
        val end = minOf(source.size - target.size, beforeIndex)
        for (i in end downTo start) {
            var match = true
            for (j in target.indices) {
                if (source[i + j] != target[j]) {
                    match = false
                    break
                }
            }
            if (match) return i
        }
        return -1
    }
}
