package framework.transformers

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class TrDouble(
    override val optional: Boolean = false,
    override val default: Double = 0.0
) : Transformer<Double> {

    override fun transform(event: MessageReceivedEvent, args: MutableList<String>): Double {
        return if (optional && args.firstOrNull()?.toDoubleOrNull() == null) {
            default
        } else {
            args.removeAt(0).toDouble()
        }
    }
}
