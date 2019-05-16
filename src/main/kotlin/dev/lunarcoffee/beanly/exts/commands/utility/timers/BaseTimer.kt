package dev.lunarcoffee.beanly.exts.commands.utility.timers

import net.dv8tion.jda.api.events.Event
import org.litote.kmongo.coroutine.CoroutineCollection
import java.util.*

interface BaseTimer {
    val time: Date

    fun <T : Any> schedule(event: Event, col: CoroutineCollection<T>)
}
