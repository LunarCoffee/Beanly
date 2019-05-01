package framework.core

import framework.api.extensions.error
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
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
        var pos = 0

        while (pos < content.length) {
            when {
                content[pos] == ' ' -> pos++
                content[pos] == '"' -> {
                    args += content.drop(pos + 1).takeWhile { it != '"' }
                    pos += args.last().length + 2
                }
                else -> {
                    args += content.drop(pos).takeWhile { it != ' ' }
                    pos += args.last().length
                }
            }
        }

        // Drop the first to remove the command name.
        return args.drop(1)
    }

    private fun suggestCommandNames(event: MessageReceivedEvent, name: String) {
        // Don't do anything if the user sent only the prefix.
        if (name.isBlank()) {
            return
        }

        for (alias in bot.commandNames) {
            if (nameDistance(name, alias) < 2) {
                GlobalScope.launch {
                    event.channel.error("That's not a command... did you mean `$alias`?") {
                        delay(5000L)
                        it.delete().queue()
                    }
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
            cur.copyInto(prev)
        }

        return cur[second.length]
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }
}
