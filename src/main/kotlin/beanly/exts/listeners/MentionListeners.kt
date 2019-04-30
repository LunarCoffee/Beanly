package beanly.exts.listeners

import beanly.consts.EMOJI_COFFEE
import framework.core.Bot
import framework.core.annotations.ListenerGroup
import framework.api.extensions.success
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

@ListenerGroup
class MentionListeners(private val bot: Bot) : ListenerAdapter() {
    override fun onMessageReceived(event: MessageReceivedEvent) {
        val content = event.message.contentRaw

        // React to the message if it contained a mention to the owner or the bot.
        if ("<@${bot.config.ownerId}>" in content) {
            event.message.addReaction(EMOJI_COFFEE).queue()
        }

        // Help the user that couldn't read the activity text by sending them the prefix. :P
        if (content == "<@${bot.jda.selfUser.id}>") {
            GlobalScope.launch {
                event.channel.success("My prefix here is `..`!")
            }
        }
    }
}
