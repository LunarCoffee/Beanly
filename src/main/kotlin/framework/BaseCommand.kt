package framework

import framework.transformers.Transformer

interface BaseCommand {
    var name: String
    var description: String

    var aliases: List<String>
    var expectedArgs: List<Transformer<out Any>>

    var deleteSender: Boolean

    var execute: suspend (CommandContext, CommandArguments) -> Unit

    fun execute(action: suspend (CommandContext, CommandArguments) -> Unit)
    fun dispatch(e: CommandContext, args: CommandArguments)
}
