package beanly.exts.listeners

import beanly.consts.Emoji
import beanly.consts.GUILD_OVERRIDES
import beanly.exts.commands.utility.GO
import framework.api.dsl.embed
import framework.api.extensions.await
import framework.core.Bot
import framework.core.annotations.ListenerGroup
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.litote.kmongo.eq
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.schedule

@ListenerGroup
class PayRespectsListeners(private val bot: Bot) : ListenerAdapter() {
    private val active = ConcurrentHashMap<String, Message>()

    override fun onMessageReceived(event: MessageReceivedEvent) {
        val messageIsNotF = !event.message.contentRaw.equals("f", ignoreCase = true)
        val guildNoPayRespects = runBlocking {
            GUILD_OVERRIDES.findOne(GO::id eq event.guild.id)?.noPayRespects
        } ?: false

        // The option to toggle the embed is provided by the <..togglef> command.
        if (event.author.isBot || messageIsNotF || guildNoPayRespects) {
            return
        }

        event.message.delete().queue()
        GlobalScope.launch {
            val message = event.channel.sendMessage(
                embed {
                    title = "${Emoji.INDICATOR_F}  Press **F** to pay respects."
                    description = "**${event.author.asTag}** has paid their respects."
                }
            ).await()

            // Add an emoji to start the chain.
            message.addReaction(Emoji.INDICATOR_F.toString()).queue()

            // Register the message and listen for reactions to it for one day.
            active[message.id] = message
            Timer().schedule(86_400_000L) { active.remove(message.id) }
        }
    }

    override fun onMessageReactionAdd(event: MessageReactionAddEvent) {
        val emote = event.reactionEmote
        if (event.user.isBot || emote.isEmoji && emote.emoji != Emoji.INDICATOR_F.toString()) {
            return
        }

        val message = active[event.messageId] ?: return
        val prev = message.embeds[0].description!!

        // Return if the user already paid their respects.
        if (event.user.asTag in prev) {
            return
        }

        event.reaction.removeReaction(event.user).queue()
        GlobalScope.launch {
            val newMessage = message.editMessage(
                embed {
                    title = "${Emoji.INDICATOR_F}  Press **F** to pay respects."
                    description = "$prev\n**${event.user.asTag}** has paid their respects."
                }
            ).await()

            // We have to set it here because apparently the object doesn't update?
            active[event.messageId] = newMessage
        }
    }
}
