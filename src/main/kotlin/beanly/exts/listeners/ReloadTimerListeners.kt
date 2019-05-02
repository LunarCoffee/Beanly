package beanly.exts.listeners

import beanly.consts.DB
import beanly.consts.MUTE_TIMERS_COL_NAME
import beanly.consts.REMIND_TIMERS_COL_NAME
import beanly.exts.commands.utility.timers.MuteTimer
import beanly.exts.commands.utility.timers.RemindTimer
import framework.core.Bot
import framework.core.annotations.ListenerGroup
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

@ListenerGroup
class ReloadTimerListeners(private val bot: Bot) : ListenerAdapter() {
    override fun onReady(event: ReadyEvent) {
        reloadRemindTimers(event)
        reloadMuteTimers(event)
    }

    private fun reloadRemindTimers(event: ReadyEvent) {
        GlobalScope.launch {
            val remindTimers = DB.getCollection<RemindTimer>(REMIND_TIMERS_COL_NAME)
            val list = remindTimers.find().toList()

            for (timer in list) {
                timer.schedule(event, remindTimers)
            }
            LOG.info("Reloaded ${list.size} reminder timer(s)!")
        }
    }

    private fun reloadMuteTimers(event: ReadyEvent) {
        GlobalScope.launch {
            val muteTimers = DB.getCollection<MuteTimer>(MUTE_TIMERS_COL_NAME)
            val list = muteTimers.find().toList()

            for (timer in list) {
                timer.schedule(event, muteTimers)
            }
            LOG.info("Reloaded ${list.size} mute timer(s)!")
        }
    }

    companion object {
        private val LOG = KotlinLogging.logger {}
    }
}
