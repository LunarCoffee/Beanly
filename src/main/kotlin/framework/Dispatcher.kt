package framework

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class Dispatcher(
    private val jda: JDA,
    private val prefix: String
) : ListenerAdapter() {

    private val commands = mutableSetOf<Command>()

    fun addCommand(command: Command) = commands.add(command)
    fun registerCommands() = jda.addEventListener(this)

    override fun onMessageReceived(e: MessageReceivedEvent) {
        if (!e.message.contentRaw.startsWith(prefix)
            || e.channelType == ChannelType.PRIVATE
            || e.channelType == ChannelType.PRIVATE
        ) {
            return
        }

        val content = e.message.contentRaw
        val name = content.substringAfter(prefix).substringBefore(" ")

        val command = commands
            .find { command -> name in command.aliases + command.name }
            ?: return

        if (command.deleteSender) {
            e.message.delete().queue()
        }

        // [rawArgs] are the raw tokens found by splitting the message by spaces (not including the
        // command name, which is technically the first argument. This list will be removed from
        // by the type transformers in [command.expectedArgs].
        val rawArgs = content
            .split("""\s+""".toRegex())
            .drop(1)
            .toMutableList()

        // This list will be added to as the arguments in [rawArgs] are transformed and removed.
        val taken = mutableListOf<String>()

        // Actually transform the arguments and execute the command with them.
        val commandArgs = command.expectedArgs.map { it.transform(rawArgs, taken) }
        command.dispatch(CommandContext(e, jda), CommandArguments(commandArgs))

        // TODO: add proper logging
        println("${e.author} used command $command!\n${command.aliases + command.name}")
    }
}
