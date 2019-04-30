package framework.core.transformers

import framework.core.transformers.utility.UserNotFound
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class TrUser(
    override val optional: Boolean = false,
    override var default: User? = null
) : Transformer<User?> {

    override fun transform(event: MessageReceivedEvent, args: MutableList<String>): User? {
        if (optional && args.isEmpty()) {
            return default
        }
        val input = args.removeAt(0)

        return when {
            input.length == 18 -> event.jda.getUserById(input)
            input.matches(""".+#\d\d\d\d$""".toRegex()) -> event.jda.getUserByTag(input)
            else -> event.jda.getUsersByName(input, true).firstOrNull()
        } ?: UserNotFound(event.jda)
    }
}
