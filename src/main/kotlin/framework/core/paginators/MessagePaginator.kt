package framework.core.paginators

import net.dv8tion.jda.api.MessageBuilder
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
        return MessageBuilder()
            .setContent("[${currentPage + 1}/$totalPages]\n${pages[currentPage].contentRaw}")
            .build()
    }
}
