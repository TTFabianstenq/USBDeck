package com.donutsmp.usbdeck.util

import java.util.Locale

object Formatters {
    fun bytes(value: Long?): String {
        if (value == null || value < 0L) return "—"
        if (value < 1024L) return "$value B"
        val units = arrayOf("KB", "MB", "GB", "TB", "PB")
        var size = value.toDouble()
        var unit = -1
        while (size >= 1024.0 && unit < units.lastIndex) {
            size /= 1024.0
            unit++
        }
        val pattern = if (size >= 100) "%.0f %s" else "%.1f %s"
        return String.format(Locale.US, pattern, size, units[unit])
    }

    fun speed(bytesPerSecond: Long): String = bytes(bytesPerSecond) + "/s"

    fun percent(used: Long?, total: Long?): Float {
        if (used == null || total == null || total <= 0L) return 0f
        return (used.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f)
    }
}
