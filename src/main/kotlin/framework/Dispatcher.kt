package framework

import framework.extensions.error
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class Dispatcher(
    private val jda: JDA,
    private val bot: Bot,
    private val prefix: String
) : ListenerAdapter() {

    private val commands = mutableSetOf<BaseCommand>()

    fun addCommand(command: BaseCommand) = commands.add(command)
    fun registerCommands() = jda.addEventListener(this)

    override fun onMessageReceived(event: MessageReceivedEvent) {
        // Keep things sane (PMs and group chats aren't allowed because I'm lazy).
        if (!event.message.contentRaw.startsWith(prefix)
            || event.author.isBot
            || event.channelType == ChannelType.PRIVATE
            || event.channelType == ChannelType.PRIVATE
        ) {
            return
        }

        val content = event.message.contentRaw
        val name = content.substringAfter(prefix).substringBefore(" ")

        val command = commands
            .find { name in it.names }
            ?: return

        // Owner only command and user ID check.
        if (command.ownerOnly && event.author.id != bot.config.ownerId) {
            GlobalScope.launch {
                event.channel.error("You need to be the owner to use that command!")
            }
            return
        }

        if (command.deleteSender) {
            event.message.delete().queue()
        }

        val rawArgs = if (command.noArgParsing) {
            mutableListOf(content.substringAfter(command.name).drop(1))
        } else {
            // This is the content split by spaces, unless there are quotes around parts of it.
            parseArgs(content).toMutableList()
        }

        // Transform argument types, and return if not all arguments were used, which means extra
        // arguments, which shouldn't be allowed.
        val commandArgs = command.expectedArgs.map { it.transform(event, rawArgs) }
        if (rawArgs.isNotEmpty()) {
            return
        }

        println(commandArgs)

        command.dispatch(CommandContext(event, jda, bot), CommandArguments(commandArgs))

        // TODO: Add proper logging with kotlin-logging.
        println("${event.author.name} used command $command!\n${command.aliases + command.name}")
    }

    private fun parseArgs(content: String): List<String> {
        val args = mutableListOf<String>()

        var currentArg = ""
        var inQuotes = false
        var prevWasQuote = false

        for (char in content) {
            if (char == '"') {
                // This is when the terminating quote comes in.
                if (inQuotes) {
                    args += currentArg
                    currentArg = ""
                    prevWasQuote = true
                }
                inQuotes = !inQuotes
                continue
            }

            // If the character doesn't mean anything special, just add it.
            if (char != ' ') {
                currentArg += char
                continue
            }

            // If the character is a space and we're in quotes, just add it. If we're not and the
            // last character was not a quote (which would be a situation like <"arg 1" arg2>),
            // we're done parsing the current arg, since it's the start of a new arg.
            if (inQuotes) {
                currentArg += " "
            } else if (!prevWasQuote) {
                args += currentArg
                currentArg = ""
            }

            prevWasQuote = false
        }

        // Remove the command name (like <..rpn>) and any trailing blank strings that interfere
        // with arglist length checking.
        return (args + currentArg).drop(1).dropLastWhile { it.isBlank() }
    }
}
