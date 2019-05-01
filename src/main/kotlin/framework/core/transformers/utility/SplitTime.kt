@file:Suppress("CanBeParameter", "MemberVisibilityCanBePrivate")

package framework.core.transformers.utility

import java.time.LocalDateTime

class SplitTime(val days: Long, val hours: Long, val minutes: Long, val seconds: Long) {
    val totalMs = days * 86_400_000 + hours * 3_600_000 + minutes * 60_000 + seconds * 1_000
    val asLocal = LocalDateTime
        .now()
        .plusDays(days)
        .plusHours(hours)
        .plusMinutes(minutes)
        .plusSeconds(seconds)!!

    override fun toString(): String {
        return arrayOf(
            if (days > 0) "$days days" else "",
            if (hours > 0) "$hours hours" else "",
            if (minutes > 0) "$minutes minutes" else "",
            if (seconds > 0) "$seconds seconds" else ""
        ).filter(String::isNotEmpty).joinToString()
    }

    companion object {
        val NONE = SplitTime(-1, -1, -1, -1)
    }
}
