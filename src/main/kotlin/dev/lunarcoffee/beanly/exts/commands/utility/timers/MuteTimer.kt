@file:Suppress("MemberVisibilityCanBePrivate")

package dev.lunarcoffee.beanly.exts.commands.utility.timers

import dev.lunarcoffee.beanly.consts.Emoji
import dev.lunarcoffee.framework.api.dsl.embed
import dev.lunarcoffee.framework.api.extensions.await
import dev.lunarcoffee.framework.api.extensions.send
import dev.lunarcoffee.framework.api.extensions.success
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq
import java.util.*
import kotlin.concurrent.schedule

class MuteTimer(
    override val time: Date,
    val guildId: String,
    val channelId: String,
    val userId: String,
    val prevRoles: List<String>,
    val mutedRole: String,
    val reason: String
) : BaseTimer {

    override fun <T : Any> schedule(event: Event, col: CoroutineCollection<T>) {
        Timer().schedule(time) {
            // Stop if the mute is no longer in the database (it has been removed manually).
            val muteStillActive = runBlocking { col.findOne(::userId eq userId) != null }
            if (!muteStillActive) {
                @Suppress("LABEL_NAME_CLASH")
                return@schedule
            }

            val user = event.jda.getUserById(userId)!!
            val guild = event.jda.getGuildById(guildId)!!

            try {
                // Remove muted role and re-add original roles.
                guild
                    .controller
                    .modifyMemberRoles(
                        guild.getMember(user)!!,
                        prevRoles.map { guild.getRoleById(it) },
                        listOf(guild.getRoleById(mutedRole))
                    )
                    .queue()

                GlobalScope.launch {
                    val channel = guild.getTextChannelById(channelId)!!
                    val pm = event.jda.getUserById(userId)!!.openPrivateChannel().await()

                    channel.success("`${user.asTag}` has been unmuted!")
                    pm.send(
                        embed {
                            title = "${Emoji.HAMMER_AND_WRENCH}  You were automatically unmuted!"
                            description = """
                                |**Server name**: ${guild.name}
                                |**Muter**: ${(event as? MessageReceivedEvent)!!.author.asTag}
                            """.trimMargin()
                        }
                    )
                }
            } finally {
                // Delete the timer so it doesn't activate on relaunch again. This is in a
                // finally block because the bot may lose permissions between the time the mute
                // command is used and when the timer to unmute runs out, causing a
                // [HierarchyException] or similar.
                GlobalScope.launch { col.deleteOne(::time eq time) }
            }
        }
    }
}
