package dev.lunarcoffee.beanly.exts.commands.utility.timers

import dev.lunarcoffee.beanly.consts.DEFAULT_TIMER
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.Event
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq
import java.util.*
import kotlin.concurrent.schedule

class RPlaceTimer(override val time: Date, val userId: String) : BaseTimer {
    override fun <T : Any> schedule(event: Event, col: CoroutineCollection<T>) {
        DEFAULT_TIMER.schedule(time) { GlobalScope.launch { col.deleteOne(::userId eq userId) } }
    }
}
