package framework.core.transformers

import framework.core.transformers.utility.SplitTime
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class TrTime(
    override val optional: Boolean = false,
    override val default: SplitTime = SplitTime.NONE
) : Transformer<SplitTime> {

    override fun transform(event: MessageReceivedEvent, args: MutableList<String>): SplitTime {
        val isTime = args.takeWhile { it.matches(TIME_REGEX) }
        if (isTime.isEmpty()) {
            throw IllegalArgumentException()
        }
        args.removeAll(isTime)

        val units = isTime
            .map { timePart ->
                TIME_REGEX.matchEntire(timePart)!!.groupValues[0].partition { it in "dhms" }.run {
                    Pair(first, second.toLong())
                }
            }
            .toMap()

        return units.run {
            SplitTime(
                getOrDefault("d", 0),
                getOrDefault("h", 0),
                getOrDefault("m", 0),
                getOrDefault("s", 0)
            )
        }
    }

    companion object {
        private val TIME_REGEX = """((\d*d)|(\d*h)|(\d*m)|(\d*s))""".toRegex()
    }
}
