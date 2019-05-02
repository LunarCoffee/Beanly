package beanly.exts.listeners

import beanly.consts.DB
import beanly.exts.commands.utility.RemindTimer
import framework.api.extensions.success
import framework.core.Bot
import framework.core.annotations.ListenerGroup
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.util.*
import kotlin.concurrent.schedule

@ListenerGroup
class ReloadTimerListeners(private val bot: Bot) : ListenerAdapter() {
    override fun onReady(event: ReadyEvent) {
        // Reload remind command timers.
        GlobalScope.launch {
            val remindTimers = DB.getCollection<RemindTimer>("RemindTimers").find().toList()
            for (timer in remindTimers) {
                timer.run {
                    Timer().schedule(this@run.time) {
                        GlobalScope.launch {
                            val channel = event
                                .jda
                                .getGuildById(guildId)!!
                                .getTextChannelById(channelId)!!
                            channel.success(
                                "Hey, $mention! Here's your reminder: `${this@run.reason}`"
                            )
                        }
                    }
                }
            }
            LOG.info("Reloaded ${remindTimers.size} reminder timers!")
        }
    }

    companion object {
        private val LOG = KotlinLogging.logger {}
    }
}
