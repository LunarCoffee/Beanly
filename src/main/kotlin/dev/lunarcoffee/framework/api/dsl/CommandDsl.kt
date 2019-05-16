package dev.lunarcoffee.framework.api.dsl

import dev.lunarcoffee.framework.core.BaseCommand
import dev.lunarcoffee.framework.core.Command

inline fun command(name: String, crossinline init: BaseCommand.() -> Unit): BaseCommand {
    return Command(name).apply(init)
}

