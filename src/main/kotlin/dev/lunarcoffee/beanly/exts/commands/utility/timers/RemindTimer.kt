@file:Suppress("MemberVisibilityCanBePrivate")

package dev.lunarcoffee.beanly.exts.commands.utility.timers

import dev.lunarcoffee.framework.api.extensions.success
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.events.Event
import org.litote.kmongo.and
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
            // Stop if the reminder is no longer in the database (it has been removed manually).
            val reminderStillActive = runBlocking { col.findOne(isSame()) != null }
            if (!reminderStillActive) {
                @Suppress("LABEL_NAME_CLASH")
                return@schedule
            }

            try {
                val channel = event
                    .jda
                    .getGuildById(guildId)!!
                    .getTextChannelById(channelId)!!

                GlobalScope.launch {
                    channel.success("Hey, $mention! Here's your reminder: `$reason`")
                }
            } finally {
                // Delete the timer so it doesn't activate on relaunch again. This must execute
                // or the reminder will be stuck forever.
                GlobalScope.launch { col.deleteOne(isSame()) }
            }
        }
    }

    fun isSame() = and(::mention eq mention, ::time eq time, ::reason eq reason)
}
