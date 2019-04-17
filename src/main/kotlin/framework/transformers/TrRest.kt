package framework.transformers

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class TrRest(
    override val optional: Boolean = false,
    override val default: String = ""
) : Transformer<String> {

    override fun transform(event: MessageReceivedEvent, args: MutableList<String>): String {
        return if (optional && args.isEmpty()) {
            default
        } else {
            args.joinToString(" ").also {
                args.clear()
            }
        }
    }
}
