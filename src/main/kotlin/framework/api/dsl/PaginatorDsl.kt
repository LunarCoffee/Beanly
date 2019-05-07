package framework.api.dsl

import framework.core.paginators.EmbedPaginator
import framework.core.paginators.MessagePaginator
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User

class MessagePaginatorDsl(creator: User) : MessagePaginator(creator) {
    fun page(content: String) {
        pages += MessageBuilder().setContent(content).build()
    }
}

class EmbedPaginatorDsl(creator: User) : EmbedPaginator(creator) {
    fun page(embed: MessageEmbed) {
        pages += MessageBuilder().setEmbed(embed).build()
    }
}

inline fun messagePaginator(
    creator: User,
    crossinline init: MessagePaginatorDsl.() -> Unit
): MessagePaginator {

    return MessagePaginatorDsl(creator).apply(init)
}

inline fun embedPaginator(
    creator: User,
    crossinline init: EmbedPaginatorDsl.() -> Unit
): EmbedPaginator {

    return EmbedPaginatorDsl(creator).apply(init)
}
