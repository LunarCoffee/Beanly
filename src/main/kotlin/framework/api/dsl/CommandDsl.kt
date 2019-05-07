package framework.api.dsl

import framework.core.BaseCommand
import framework.core.Command

inline fun command(name: String, crossinline init: BaseCommand.() -> Unit): BaseCommand {
    return Command(name).apply(init)
}

