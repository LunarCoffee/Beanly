package dev.lunarcoffee.framework.core.paginators

import net.dv8tion.jda.api.MessageBuilder
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
        return if (totalPages == 1) {
            MessageBuilder().setEmbed(pages[currentPage].embeds[0]).build()
        } else {
            MessageBuilder()
                .setContent("[${currentPage + 1}/$totalPages]")
                .setEmbed(pages[currentPage].embeds[0])
                .build()
        }
    }
}
