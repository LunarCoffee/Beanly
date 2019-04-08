package framework.dsl

import framework.Command

fun command(name: String, init: Command.() -> Unit) = Command(name).apply(init)
