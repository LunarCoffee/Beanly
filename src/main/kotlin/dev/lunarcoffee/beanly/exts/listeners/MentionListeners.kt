package dev.lunarcoffee.beanly.exts.listeners

import dev.lunarcoffee.beanly.consts.Emoji
import dev.lunarcoffee.framework.api.extensions.success
import dev.lunarcoffee.framework.core.Bot
import dev.lunarcoffee.framework.core.annotations.ListenerGroup
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

@ListenerGroup
class MentionListeners(private val bot: Bot) : ListenerAdapter() {
    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        val content = event.message.contentRaw

        // React to the message if it contained a mention to the owner or the bot.
        if ("<@${bot.config.ownerId}>" in content) {
            event.message.addReaction(Emoji.COFFEE.toString()).queue()
        }

        // Help the user that couldn't read the activity text by sending them the prefix. :P
        if (content == "<@${bot.jda.selfUser.id}>") {
            GlobalScope.launch { event.channel.success("My prefix here is `..`!") }
        }
    }
}
