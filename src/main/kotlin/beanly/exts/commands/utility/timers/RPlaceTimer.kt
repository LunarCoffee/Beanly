package beanly.exts.commands.utility.timers

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.Event
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq
import java.util.*
import kotlin.concurrent.schedule

class RPlaceTimer(override val time: Date, val userId: String) : BaseTimer {
    override fun <T : Any> schedule(event: Event, col: CoroutineCollection<T>) {
        Timer().schedule(time) { GlobalScope.launch { col.deleteOne(::userId eq userId) } }
    }
}
