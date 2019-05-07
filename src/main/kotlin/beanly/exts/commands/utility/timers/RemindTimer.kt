@file:Suppress("MemberVisibilityCanBePrivate")

package beanly.exts.commands.utility.timers

import framework.api.extensions.success
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.Event
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq
import java.util.*
import kotlin.concurrent.schedule

class RemindTimer(
    override val time: Date,
    val guildId: String,
    val channelId: String,
    val mention: String,
    val reason: String
) : BaseTimer {

    override fun <T : Any> schedule(event: Event, col: CoroutineCollection<T>) {
        Timer().schedule(time) {
            GlobalScope.launch {
                try {
                    val channel = event
                        .jda
                        .getGuildById(guildId)!!
                        .getTextChannelById(channelId)!!
                    channel.success("Hey, $mention! Here's your reminder: `$reason`")
                } finally {
                    // Delete the timer so it doesn't activate on relaunch again. This must execute
                    // or the reminder will be stuck forever.
                    col.deleteOne(::time eq time)
                }
            }
        }
    }
}
