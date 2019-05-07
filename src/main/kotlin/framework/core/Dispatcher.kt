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
        // Keep things sane (PMs and group chats aren't allowed for now because I'm lazy).
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
                sendUsage(event, name)
                return
            }
        }

        // Return if not all arguments were used, which means extra arguments were given, which
        // shouldn't be allowed. Ask them to use the help command.
        if (rawArgs.isNotEmpty()) {
            sendUsage(event, name)
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

    private fun sendUsage(event: MessageReceivedEvent, name: String) {
        GlobalScope.launch {
            event.channel.error("That's not quite right. Type `..help $name` for more info.") {
                delay(5000L)
                it.delete().queue()
            }
        }
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }
}
