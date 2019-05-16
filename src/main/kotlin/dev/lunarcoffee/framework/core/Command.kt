package dev.lunarcoffee.framework.core

import dev.lunarcoffee.framework.core.transformers.Transformer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class Command(override var name: String) : BaseCommand {
    override var groupName = "Misc"

    override var description = "(no description)"
    override var extDescription = "(no extended description)"

    override var aliases = listOf<String>()
    override var expectedArgs = emptyList<Transformer<out Any?>>()

    override val names get() = aliases + name

    override var deleteSender = false
    override var ownerOnly = false
    override var noArgParsing = false

    override lateinit var execute: suspend (CommandContext, CommandArguments) -> Unit

    override fun execute(action: suspend (CommandContext, CommandArguments) -> Unit) {
        execute = action
    }

    override fun dispatch(ctx: CommandContext, args: CommandArguments) {
        GlobalScope.launch { execute(ctx, args) }
    }

    override fun toString() = "Command(name=$name, desc=$description, aliases=$aliases)"
}
