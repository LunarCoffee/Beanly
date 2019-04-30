package framework.core

import framework.core.transformers.Transformer

interface BaseCommand {
    var name: String
    var groupName: String
    var description: String
    var extDescription: String

    var aliases: List<String>
    val names: List<String>
    var expectedArgs: List<Transformer<out Any?>>

    var deleteSender: Boolean
    var ownerOnly: Boolean
    var noArgParsing: Boolean

    var execute: suspend (CommandContext, CommandArguments) -> Unit

    fun execute(action: suspend (CommandContext, CommandArguments) -> Unit)
    fun dispatch(ctx: CommandContext, args: CommandArguments)
}
