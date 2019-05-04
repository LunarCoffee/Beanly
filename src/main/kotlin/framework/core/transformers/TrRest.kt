package framework.core.transformers

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class TrRest(
    override val optional: Boolean = false,
    override val default: String = ""
) : Transformer<String> {

    override fun transform(event: MessageReceivedEvent, args: MutableList<String>): String {
        val joined = args.joinToString(" ")
        if (!optional && joined.isEmpty()) {
            throw IllegalArgumentException()
        }

        return if (optional && args.isEmpty()) {
            default
        } else {
            args.clear()
            joined
        }
    }
}
