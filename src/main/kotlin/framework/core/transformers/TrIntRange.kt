package framework.core.transformers

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class TrIntRange(
    override val optional: Boolean = false,
    override val default: IntRange = 0..0
) : Transformer<IntRange> {

    override fun transform(event: MessageReceivedEvent, args: MutableList<String>): IntRange {
        val match = INT_RANGE.matchEntire(args.firstOrNull() ?: "")

        return if (optional && match == null) {
            default
        } else {
            val (startStr, endStr) = match?.destructured ?: return returnOrThrow()
            val start = startStr.toIntOrNull() ?: return returnOrThrow()
            val end = endStr.toIntOrNull() ?: return returnOrThrow()

            println("$start $end")

            if (start >= end) {
                return returnOrThrow()
            }
            start..end
        }
    }

    // Returns if the argument is optional, throws an exception otherwise.
    private fun returnOrThrow(): IntRange {
        return if (optional) {
            default
        } else {
            throw IllegalArgumentException()
        }
    }

    companion object {
        private val INT_RANGE = """(\d+)-(\d+)""".toRegex()
    }
}
