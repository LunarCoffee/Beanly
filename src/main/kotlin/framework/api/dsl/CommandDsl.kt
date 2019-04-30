package framework.api.dsl

import framework.core.BaseCommand
import framework.core.Command

fun command(name: String, init: BaseCommand.() -> Unit): BaseCommand = Command(name).apply(init)
