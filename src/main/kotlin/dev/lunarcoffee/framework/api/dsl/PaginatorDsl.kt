package dev.lunarcoffee.framework.api.dsl

import dev.lunarcoffee.framework.core.paginators.EmbedPaginator
import dev.lunarcoffee.framework.core.paginators.MessagePaginator
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User

class MessagePaginatorDsl(creator: User) : MessagePaginator(creator) {
    fun page(content: String) {
        pages += message { this@message.content = content }
    }
}

class EmbedPaginatorDsl(creator: User) : EmbedPaginator(creator) {
    fun page(embed: MessageEmbed) {
        pages += message { this@message.embed = embed }
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
