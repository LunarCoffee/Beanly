package dev.lunarcoffee.framework.core.transformers

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

interface Transformer<T> {
    val optional: Boolean
    val default: T

    fun transform(event: MessageReceivedEvent, args: MutableList<String>): T
}
