package framework.api.extensions

import framework.core.paginators.Paginator
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.exceptions.ErrorResponseException

suspend fun MessageChannel.send(msg: String, after: suspend (Message) -> Unit = {}) {
    try {
        after(sendMessage(msg).await())
    } catch (e: ErrorResponseException) {
        error("The message that was supposed to be sent can't fit in a message!")
    }
}

suspend fun MessageChannel.send(embed: MessageEmbed, after: suspend (Message) -> Unit = {}) {
    try {
        after(sendMessage(embed).await())
    } catch (e: ErrorResponseException) {
        error("The message that was supposed to be sent can't fit in an embed!")
    }
}

suspend fun MessageChannel.send(paginator: Paginator, after: suspend (Paginator) -> Unit = {}) {
    try {
        after(paginator.apply { send(this@send) })
    } catch (e: ErrorResponseException) {
        error("The message that was supposed to be sent can't fit in an embed!")
    }
}

suspend fun MessageChannel.success(msg: String, after: suspend (Message) -> Unit = {}) {
    send(":white_check_mark:  $msg  **\\o/**", after)
}

suspend fun MessageChannel.error(msg: String, after: suspend (Message) -> Unit = {}) {
    send(":negative_squared_cross_mark:  $msg  **>~<**", after)
}
