package framework.api.dsl

import framework.api.paginators.EmbedPaginator
import framework.api.paginators.MessagePaginator
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

fun messagePaginator(creator: User, init: MessagePaginatorDsl.() -> Unit): MessagePaginator {
    return MessagePaginatorDsl(creator).apply(init)
}

fun embedPaginator(creator: User, init: EmbedPaginatorDsl.() -> Unit): EmbedPaginator {
    return EmbedPaginatorDsl(creator).apply(init)
}
