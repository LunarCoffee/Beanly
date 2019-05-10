package framework.core.transformers

import framework.core.transformers.utility.SplitTime
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class TrTime(
    override val optional: Boolean = false,
    override val default: SplitTime = SplitTime.NONE
) : Transformer<SplitTime> {

    override fun transform(event: MessageReceivedEvent, args: MutableList<String>): SplitTime {
        if (args.isEmpty()) {
            if (optional) {
                return default
            }
            throw IllegalArgumentException()
        }

        // Try checking for a calendar/clock time (i.e. "July 20 4:31 PM").
        try {
            val year = LocalDateTime.now().year
            // TODO: also fill in month and day and add to help texts
            val time = HUMAN_TIME_FORMATTER.parse("${args[0]} $year EST")

            return try {
                SplitTime(Instant.from(time).toEpochMilli() - System.currentTimeMillis())
            } catch (e: Exception) {
                println(e.message)
                throw e
            }
        } catch (e: DateTimeParseException) {
            // Ignored; the code should continue to the next tests.
        }

        // Start checking for a interval of time, since it wasn't a calendar/clock form thing.
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
        private val HUMAN_TIME_FORMATTER = DateTimeFormatter.ofPattern("[MMM dd ]hh:mm a yyyy z")
    }
}
