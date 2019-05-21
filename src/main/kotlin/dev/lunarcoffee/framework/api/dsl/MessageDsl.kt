package dev.lunarcoffee.framework.api.dsl

import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.MessageEmbed

class MessageDsl {
    var content = ""
    var embed: MessageEmbed? = null

    fun create() = MessageBuilder().setContent(content).setEmbed(embed).build()
}

inline fun message(crossinline init: MessageDsl.() -> Unit) = MessageDsl().apply(init).create()
