package dev.lunarcoffee.beanly

fun String.trimToDescription() = trimMargin().replace("\n", " ").replace("\\n", "\n")
fun String.constToEng() = replace("_", "").toLowerCase()

fun <T : Enum<T>> Enum<T>.constToEng() = name.replace("_", "").toLowerCase()
