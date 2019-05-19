package dev.lunarcoffee.beanly

fun Boolean.toYesNo() = if (this) "yes" else "no"

fun String.trimToDescription() = trimMargin().replace("\n", " ").replace("\\n", "\n")
fun String.constToEng() = replace("_", " ").toLowerCase()

fun <T : Enum<T>> Enum<T>.constToEng() = name.replace("_", " ").toLowerCase()
