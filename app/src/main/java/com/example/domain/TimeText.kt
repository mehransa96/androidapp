package com.example.domain

/**
 * Reads a time of day typed by the user ("14:30", "۱۴:۳۰", "14.30", "9", "0930") and returns it as
 * "HH:mm", or null when it is not a valid time. Alarms used to be scheduled from the raw text, so a
 * time typed with Persian digits rang at 08:00 and "14" was never scheduled at all.
 */
object TimeText {

    fun normalize(input: String): String? {
        val ascii = buildString {
            input.trim().forEach { c ->
                when (c) {
                    in '۰'..'۹' -> append('0' + (c - '۰'))
                    in '٠'..'٩' -> append('0' + (c - '٠'))
                    in '0'..'9' -> append(c)
                    ':', '.', '٫', '/', '-', ' ', '：' -> append(':')
                }
            }
        }.trim(':')
        if (ascii.isEmpty()) return null
        val parts = ascii.split(':').filter { it.isNotEmpty() }
        val (h, m) = when {
            parts.size == 2 -> parts[0].toIntOrNull() to parts[1].toIntOrNull()
            parts.size == 1 && parts[0].length <= 2 -> parts[0].toIntOrNull() to 0
            parts.size == 1 && parts[0].length in 3..4 ->
                parts[0].dropLast(2).toIntOrNull() to parts[0].takeLast(2).toIntOrNull()
            else -> null to null
        }
        if (h == null || m == null || h !in 0..23 || m !in 0..59) return null
        return "%02d:%02d".format(h, m)
    }

    /** Hour and minute of a stored time, or null when it cannot be read. */
    fun parse(input: String): Pair<Int, Int>? = normalize(input)?.let {
        it.substring(0, 2).toInt() to it.substring(3, 5).toInt()
    }
}
