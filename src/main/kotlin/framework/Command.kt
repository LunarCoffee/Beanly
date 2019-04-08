package framework

import framework.transformers.Transformer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

data class Command(val name: String, var description: String = "(no description)") {
    var expectedArgs = emptyList<Transformer<out Any>>()

    var aliases = listOf<String>()
    var deleteSender = false
    private var execute: suspend (CommandContext, CommandArguments) -> Unit = { _, _ -> }

    fun execute(action: suspend (CommandContext, CommandArguments) -> Unit) {
        execute = action
    }

    fun dispatch(e: CommandContext, args: CommandArguments) {
        GlobalScope.launch {
            execute(e, args)
        }
    }

    override fun toString() = "Command(name=$name, desc=$description, aliases=$aliases)"
}
