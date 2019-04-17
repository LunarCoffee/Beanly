package framework.transformers

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class TrInt(
    override val optional: Boolean = false,
    override val default: Int = 0,
    override val name: String = "number"
) : Transformer<Int> {

    override fun transform(event: MessageReceivedEvent, args: MutableList<String>): Int {
        return if (optional && args.firstOrNull()?.toIntOrNull() == null) {
            default
        } else {
            args.removeAt(0).toInt()
        }
    }

    override fun toString() = name
}
