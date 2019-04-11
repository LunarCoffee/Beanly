package framework.extensions

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.exceptions.ErrorResponseException

suspend fun MessageChannel.send(msg: String, after: (Message) -> Unit = {}) {
    try {
        after(sendMessage(msg).await())
    } catch (e: ErrorResponseException) {
        error("The message that was supposed to be sent can't fit in a message!")
    }
}

suspend fun MessageChannel.send(embed: MessageEmbed, after: (Message) -> Unit = {}) {
    try {
        after(sendMessage(embed).await())
    } catch (e: ErrorResponseException) {
        error("The message that was supposed to be sent can't fit in an embed!")
    }
}

suspend fun MessageChannel.success(msg: String) = send(":white_check_mark:  $msg  **\\o/**")
suspend fun MessageChannel.error(msg: String) = send(":negative_squared_cross_mark:  $msg  **>~<**")
