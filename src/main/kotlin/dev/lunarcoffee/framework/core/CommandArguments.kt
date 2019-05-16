package dev.lunarcoffee.framework.core

class CommandArguments(private val items: List<Any?>) {
    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(index: Int) = items[index] as T
}
