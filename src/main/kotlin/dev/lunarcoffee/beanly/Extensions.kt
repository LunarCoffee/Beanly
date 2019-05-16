package dev.lunarcoffee.beanly

import java.time.OffsetDateTime

fun OffsetDateTime.gmtToEst() = minusHours(4)!!

fun String.trimToDescription() = trimMargin().replace("\n", " ").replace("\\n", "\n")
