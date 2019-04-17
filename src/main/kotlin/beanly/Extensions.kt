package beanly

import java.time.OffsetDateTime

fun <T> Iterable<T>.ifEmptyToString(): String {
    return if (this.count() == 0) {
        "(none)"
    } else {
        toString()
    }
}

fun OffsetDateTime.gmtToEst() = minusHours(4)!!

fun String.trimToDescription() = trimMargin().replace("\n", " ").replace("\\n", "\n")
