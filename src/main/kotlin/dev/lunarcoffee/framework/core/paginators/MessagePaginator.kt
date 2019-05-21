package dev.lunarcoffee.framework.core.paginators

import dev.lunarcoffee.framework.api.dsl.message
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import java.util.*

open class MessagePaginator(override val creator: User) : Paginator() {
    override val closeTimer = Timer()
    override lateinit var closeTask: TimerTask

    override lateinit var message: Message

    override val pages = mutableListOf<Message>()
    override var currentPage = 0

    override fun formatMessage(): Message {
        return message {
            content = if (totalPages == 1) {
                pages[currentPage].contentRaw
            } else {
                "[${currentPage + 1}/$totalPages]\n${pages[currentPage].contentRaw}"
            }
        }
    }
}
