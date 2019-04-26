package framework.transformers

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class TrGreedy<T>(
    val conversionFunction: (String) -> T,
    vararg defaults: T
) : Transformer<List<T>> {

    // Greedy is always technically optional, since it can steal at least 0 args.
    override val optional = true
    override val default = defaults.toList()

    override fun transform(event: MessageReceivedEvent, args: MutableList<String>): List<T> {
        if (args.isEmpty()) {
            return default
        }

        val result = mutableListOf<T>()
        var numTaken = 0

        for (arg in args) {
            try {
                val item = args[0 + numTaken++]
                result.add(conversionFunction(item))
            } catch (e: Exception) {
                break
            }
        }
        args.removeAll(args.take(numTaken))

        return if (result.isEmpty()) {
            default
        } else {
            result
        }
    }
}
