package dev.lunarcoffee.beanly.exts.listeners.specific

import dev.lunarcoffee.framework.core.Bot
import dev.lunarcoffee.framework.core.annotations.ListenerGroup
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

@ListenerGroup
class TardCardsJoinListeners(private val bot: Bot) : ListenerAdapter() {
    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        // Only affect one server.
        if (event.guild.id != "579464256882343946") {
            return
        }

        // Send based on user ID.
        event.guild.getTextChannelById(579467360914898954)!!.sendMessage(
            if (event.user.id == "239793934123925507") {
                "The ultimate enemy has arrived! Ready the weapons!"
            } else {
                "${event.user.asMention} has joined the rebellion!"
            }
        ).queue()
    }
}
