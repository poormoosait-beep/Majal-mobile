package com.majal.mobile

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.net.Socket
import javax.net.ssl.SSLSocketFactory

class RouterOsApi(
    private val host: String,
    private val port: Int,
    private val username: String,
    private val password: String,
    private val useTls: Boolean = true
) : AutoCloseable {
    private val socket: Socket = if (useTls) {
        SSLSocketFactory.getDefault().createSocket(host, port)
    } else Socket(host, port)
    private val input = BufferedInputStream(socket.getInputStream())
    private val output = BufferedOutputStream(socket.getOutputStream())

    init {
        socket.soTimeout = 10000
        writeSentence(listOf("/login", "=name=$username", "=password=$password"))
        val login = readReply()
        if (login.none { it.firstOrNull() == "!done" }) {
            close()
            throw IllegalStateException("ورود به RouterOS ناموفق بود؛ نام کاربری، رمز یا نسخه API را بررسی کن.")
        }
    }

    @Synchronized fun command(vararg words: String): List<Map<String, String>> {
        writeSentence(words.toList())
        val rows = mutableListOf<Map<String, String>>()
        while (true) {
            val sentence = readSentence()
            if (sentence.isEmpty()) continue
            when (sentence[0]) {
                "!re" -> {
                    val row = linkedMapOf<String, String>()
                    sentence.drop(1).forEach { word ->
                        if (word.startsWith("=")) {
                            val split = word.indexOf('=', 1)
                            if (split > 1) row[word.substring(1, split)] = word.substring(split + 1)
                        }
                    }
                    rows.add(row)
                }
                "!trap" -> throw IllegalStateException(sentence.firstOrNull { it.startsWith("=message=") }?.substringAfter("=message=") ?: "RouterOS command failed")
                "!fatal" -> throw IllegalStateException("ارتباط API توسط روتر بسته شد.")
                "!done" -> return rows
            }
        }
    }

    private fun readReply(): List<List<String>> {
        val result = mutableListOf<List<String>>()
        while (true) {
            val s = readSentence()
            result.add(s)
            if (s.firstOrNull() == "!done" || s.firstOrNull() == "!fatal") return result
        }
    }

    private fun writeSentence(words: List<String>) {
        words.forEach { word ->
            val bytes = word.toByteArray(Charsets.UTF_8)
            writeLength(bytes.size)
            output.write(bytes)
        }
        output.write(0)
        output.flush()
    }

    private fun writeLength(value: Int) {
        when {
            value < 0x80 -> output.write(value)
            value < 0x4000 -> { output.write((value shr 8) or 0x80); output.write(value and 0xff) }
            value < 0x200000 -> { output.write((value shr 16) or 0xc0); output.write((value shr 8) and 0xff); output.write(value and 0xff) }
            value < 0x10000000 -> { output.write((value shr 24) or 0xe0); output.write((value shr 16) and 0xff); output.write((value shr 8) and 0xff); output.write(value and 0xff) }
            else -> { output.write(0xf0); output.write((value shr 24) and 0xff); output.write((value shr 16) and 0xff); output.write((value shr 8) and 0xff); output.write(value and 0xff) }
        }
    }

    private fun readLength(): Int {
        val c = input.read()
        if (c < 0) throw IllegalStateException("اتصال روتر قطع شد.")
        return when {
            c and 0x80 == 0 -> c
            c and 0xc0 == 0x80 -> ((c and 0x3f) shl 8) or readByte()
            c and 0xe0 == 0xc0 -> ((c and 0x1f) shl 16) or (readByte() shl 8) or readByte()
            c and 0xf0 == 0xe0 -> ((c and 0x0f) shl 24) or (readByte() shl 16) or (readByte() shl 8) or readByte()
            else -> (readByte() shl 24) or (readByte() shl 16) or (readByte() shl 8) or readByte()
        }
    }
    private fun readByte(): Int {
        val b = input.read()
        if (b < 0) throw IllegalStateException("اتصال روتر قطع شد.")
        return b
    }
    private fun readSentence(): List<String> {
        val words = mutableListOf<String>()
        while (true) {
            val len = readLength()
            if (len == 0) return words
            val bytes = ByteArray(len)
            var offset = 0
            while (offset < len) {
                val count = input.read(bytes, offset, len - offset)
                if (count < 0) throw IllegalStateException("پاسخ روتر ناقص دریافت شد.")
                offset += count
            }
            words.add(String(bytes, Charsets.UTF_8))
        }
    }
    override fun close() { try { socket.close() } catch (_: Exception) {} }
}
