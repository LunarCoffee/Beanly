package framework

import framework.transformers.Transformer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class Command(
    override var name: String,
    override var description: String = "(no description)"
) : BaseCommand {

    override var aliases = listOf<String>()
    override var expectedArgs = emptyList<Transformer<out Any>>()

    override val names
        get() = aliases + name

    override var deleteSender = false
    override var ownerOnly = false

    override var execute: suspend (CommandContext, CommandArguments) -> Unit = { _, _ -> }

    override fun execute(action: suspend (CommandContext, CommandArguments) -> Unit) {
        execute = action
    }

    override fun dispatch(e: CommandContext, args: CommandArguments) {
        GlobalScope.launch {
            execute(e, args)
        }
    }

    override fun toString() = "Command(name=$name, desc=$description, aliases=$aliases)"
}
