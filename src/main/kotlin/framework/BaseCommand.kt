package framework

import framework.transformers.Transformer

interface BaseCommand {
    var name: String
    var description: String
    var extDescription: String

    var aliases: List<String>
    val names: List<String>
    var expectedArgs: List<Transformer<out Any>>

    var deleteSender: Boolean
    var ownerOnly: Boolean

    var execute: suspend (CommandContext, CommandArguments) -> Unit

    fun execute(action: suspend (CommandContext, CommandArguments) -> Unit)
    fun dispatch(e: CommandContext, args: CommandArguments)
}
