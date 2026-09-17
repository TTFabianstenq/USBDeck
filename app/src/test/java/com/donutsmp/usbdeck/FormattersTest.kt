package com.donutsmp.usbdeck

import com.donutsmp.usbdeck.util.Formatters
import com.donutsmp.usbdeck.util.Sha256
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormattersTest {
    @Test
    fun bytesNullIsEmDash() {
        assertEquals("—", Formatters.bytes(null))
    }

    @Test
    fun bytesFormatsGigabytes() {
        assertEquals("1.0 GB", Formatters.bytes(1024L * 1024L * 1024L))
    }

    @Test
    fun percentClamps() {
        assertEquals(0f, Formatters.percent(null, 100), 0.0f)
        assertEquals(0.5f, Formatters.percent(50, 100), 0.0f)
    }

    @Test
    fun sha256KnownVector() {
        val hash = Sha256.digest("abc".toByteArray())
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", hash)
        assertTrue(Sha256.matches(hash, hash.uppercase()))
        assertFalse(Sha256.matches("nope", hash))
    }
}
