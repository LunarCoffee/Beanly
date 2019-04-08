package framework.dsl

import framework.BaseCommand
import framework.Command

fun command(name: String, init: BaseCommand.() -> Unit): BaseCommand = Command(name).apply(init)
