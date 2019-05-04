@file:Suppress("MemberVisibilityCanBePrivate")

package beanly.exts.commands.utility.timers

import framework.api.extensions.await
import framework.api.extensions.success
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.Event
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
            val user = event.jda.getUserById(userId)!!
            val guild = event.jda.getGuildById(guildId)!!

            // Remove muted role and add original role.
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
                pm.success("You have been unmuted in **${guild.name}**!")

                // Delete the timer so it doesn't activate on relaunch again.
                col.deleteOne(::time eq time)
            }
        }
    }
}
