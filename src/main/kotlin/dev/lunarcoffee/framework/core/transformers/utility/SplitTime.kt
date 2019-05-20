@file:Suppress("MemberVisibilityCanBePrivate")

package dev.lunarcoffee.framework.core.transformers.utility

import dev.lunarcoffee.beanly.consts.TIME_FORMATTER
import java.time.LocalDateTime

class SplitTime(val days: Long, val hours: Long, val minutes: Long, val seconds: Long) {
    // Constructs from a total number of milliseconds.
    constructor(m: Long) : this(m / 86_400_000, m / 3_600_000 % 24, m / 60_000 % 60, m / 1000 % 60)

    val totalMs = days * 86_400_000 + hours * 3_600_000 + minutes * 60_000 + seconds * 1_000
    val asLocal = LocalDateTime
        .now()
        .plusDays(days)
        .plusHours(hours)
        .plusMinutes(minutes)
        .plusSeconds(seconds)!!

    fun localWithoutWeekday() = asLocal.format(TIME_FORMATTER).drop(4)

    override fun toString(): String {
        val mtoDay = if (days != 1L) "s" else ""
        val mtoHour = if (hours != 1L) "s" else ""
        val mtoMinute = if (minutes != 1L) "s" else ""
        val mtoSecond = if (seconds != 1L) "s" else ""

        return if (totalMs == 0L) {
            "0 seconds"
        } else {
            arrayOf(
                if (days > 0) "$days day$mtoDay" else "",
                if (hours > 0) "$hours hour$mtoHour" else "",
                if (minutes > 0) "$minutes minute$mtoMinute" else "",
                if (seconds > 0) "$seconds second$mtoSecond" else ""
            ).filter(String::isNotEmpty).joinToString()
        }
    }

    companion object {
        val NONE = SplitTime(-1, -1, -1, -1)
    }
}
