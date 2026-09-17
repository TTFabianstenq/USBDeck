package com.donutsmp.usbdeck.util

import java.io.InputStream
import java.security.MessageDigest

object Sha256 {
    fun digest(input: InputStream, bufferSize: Int = 64 * 1024): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(bufferSize)
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            digest.update(buffer, 0, read)
        }
        return digest.digest().joinToString("") { b -> "%02x".format(b) }
    }

    fun digest(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(bytes).joinToString("") { b -> "%02x".format(b) }
    }

    fun matches(expected: String?, actual: String?): Boolean {
        if (expected.isNullOrBlank() || actual.isNullOrBlank()) return false
        return expected.trim().equals(actual.trim(), ignoreCase = true)
    }
}
