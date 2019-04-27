package framework

import framework.extensions.error
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import kotlin.math.min

class Dispatcher(
    private val jda: JDA,
    private val bot: Bot,
    private val prefix: String
) : ListenerAdapter() {

    private val commands = mutableSetOf<BaseCommand>()

    fun addCommand(command: BaseCommand) = commands.add(command)

    fun registerCommands() {
        jda.removeEventListener(this)
        jda.addEventListener(this)
    }

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
            ?: return suggestCommandNames(event, name)

        // Owner only command and user ID check.
        if (command.ownerOnly && event.author.id != bot.config.ownerId) {
            GlobalScope.launch {
                event.channel.error("You need to be the owner to use that command!")
            }
            log.info { "${event.author.name} tried to use owner-only command $command!" }
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

        // Transform arguments into their types, using exceptions to determine when a non-optional
        // transformation failed.
        val commandArgs = command.expectedArgs.map {
            try {
                it.transform(event, rawArgs)
            } catch (e: Exception) {
                log.info { "${event.author.name} used command $command with incorrect args." }
                return
            }
        }

        // Return if not all arguments were used, which means extra arguments were given, which
        // shouldn't be allowed.
        if (rawArgs.isNotEmpty()) {
            return
        }

        command.dispatch(CommandContext(event, jda, bot), CommandArguments(commandArgs))
        log.info { "${event.author.name} used command $command with args $commandArgs" }
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

        // Remove the command name (like <..iss>) and any trailing blank strings that interfere
        // with arglist length checking.
        return (args + currentArg).drop(1).dropLastWhile { it.isBlank() }
    }

    private fun suggestCommandNames(event: MessageReceivedEvent, name: String) {
        // Don't do anything if the user sent only the prefix.
        if (name.isBlank()) {
            return
        }

        for (alias in bot.commandNames) {
            if (nameDistance(name, alias) < 2) {
                GlobalScope.launch {
                    event.channel.error("That's not a command... did you mean `$alias`?")
                }
                return
            }
        }
    }

    private fun nameDistance(first: String, second: String): Int {
        val prev = IntArray(second.length + 1) { it }
        val cur = IntArray(second.length + 1)
        var cost: Int

        for (i in 0 until first.length) {
            cur[0] = i + 1
            for (j in 0 until second.length) {
                cost = if (first[i] == second[j]) 0 else 1
                cur[j + 1] = min(cur[j] + 1, min(prev[j + 1] + 1, prev[j] + cost))
            }
            for (j in 0..second.length) prev[j] = cur[j]
        }

        return cur[second.length]
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }
}
