package framework

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

// This cast will always succeed, since this class is only instantiated by a [Dispatcher], which
// stops processing a message event if it is a group or private chat.
class CommandContext(
    private val event: MessageReceivedEvent,
    val jda: JDA
) : TextChannel by event.channel as TextChannel
