package dev.lunarcoffee.framework.core.paginators

import dev.lunarcoffee.framework.api.dsl.message
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import java.util.*

open class EmbedPaginator(override val creator: User) : Paginator() {
    override val closeTimer = Timer()
    override lateinit var closeTask: TimerTask

    override lateinit var message: Message

    override val pages = mutableListOf<Message>()
    override var currentPage = 0

    override fun formatMessage(): Message {
        return message {
            if (totalPages == 1) {
                embed = pages[currentPage].embeds[0]
            } else {
                content = "[${currentPage + 1}/$totalPages]"
                embed = pages[currentPage].embeds[0]
            }
        }
    }
}
