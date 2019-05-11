package beanly.exts.listeners

import beanly.consts.GUILD_OVERRIDES
import beanly.exts.commands.utility.GO
import framework.api.extensions.error
import framework.core.Bot
import framework.core.annotations.ListenerGroup
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.litote.kmongo.eq
import kotlin.math.min

@ListenerGroup
class CommandSuggestionListeners(private val bot: Bot) : ListenerAdapter() {
    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        val notBeanlyCommand = !event.message.contentRaw.startsWith(bot.config.prefix)
        val noCommandSuggestions = runBlocking {
            GUILD_OVERRIDES.findOne(GO::id eq event.guild.id)?.noSuggestCommands
        } ?: false

        if (notBeanlyCommand || event.author.isBot || noCommandSuggestions) {
            return
        }

        val name = event.message.contentRaw.substringAfter(bot.config.prefix).substringBefore(" ")
        if (name !in bot.commands.flatMap { it.names }) {
            suggestCommandNames(event, name)
        }
    }

    private fun suggestCommandNames(event: GuildMessageReceivedEvent, name: String) {
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
}
