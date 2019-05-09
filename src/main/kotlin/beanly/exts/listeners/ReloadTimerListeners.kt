package beanly.exts.listeners

import beanly.consts.BEANLY_CONFIG
import beanly.consts.COL_NAMES
import beanly.consts.DB
import beanly.exts.commands.utility.timers.BaseTimer
import beanly.exts.commands.utility.timers.MuteTimer
import beanly.exts.commands.utility.timers.RPlaceTimer
import beanly.exts.commands.utility.timers.RemindTimer
import framework.core.Bot
import framework.core.annotations.ListenerGroup
import framework.core.silence
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.io.File
import java.lang.IllegalArgumentException

@ListenerGroup
class ReloadTimerListeners(private val bot: Bot) : ListenerAdapter() {
    private val cl = ClassLoader.getSystemClassLoader()

    override fun onReady(event: ReadyEvent) {
        GlobalScope.launch {
            File(bot.config.sourceRootDir)
                .walk()
                .mapNotNull {
                    silence {
                        cl.loadClass("${BEANLY_CONFIG.timerPath}.${it.nameWithoutExtension}")
                    }
                }
                .filter { c -> BaseTimer::class.java in c.interfaces }
                .forEach {
                    val name = COL_NAMES[it.simpleName]!!
                    val timerCol = when (it.simpleName) {
                        "MuteTimer" -> DB.getCollection<MuteTimer>(name)
                        "RemindTimer" -> DB.getCollection<RemindTimer>(name)
                        "RPlaceTimer" -> DB.getCollection<RPlaceTimer>(name)
                        else -> throw IllegalArgumentException()
                    }
                    val list = timerCol.find().toList()

                    for (timer in list) {
                        timer.schedule(event, timerCol)
                    }
                    LOG.info("Reloaded ${list.size} timer(s) from $name!")
                }
            LOG.info("Finished reloading timers!")
        }
    }

    companion object {
        private val LOG = KotlinLogging.logger {}
    }
}
