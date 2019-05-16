package dev.lunarcoffee.beanly.exts.listeners

import dev.lunarcoffee.beanly.consts.BEANLY_CONFIG
import dev.lunarcoffee.beanly.consts.COL_NAMES
import dev.lunarcoffee.beanly.consts.DB
import dev.lunarcoffee.beanly.exts.commands.utility.timers.BaseTimer
import dev.lunarcoffee.beanly.exts.commands.utility.timers.MuteTimer
import dev.lunarcoffee.beanly.exts.commands.utility.timers.RPlaceTimer
import dev.lunarcoffee.beanly.exts.commands.utility.timers.RemindTimer
import dev.lunarcoffee.framework.core.Bot
import dev.lunarcoffee.framework.core.annotations.ListenerGroup
import dev.lunarcoffee.framework.core.silence
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
