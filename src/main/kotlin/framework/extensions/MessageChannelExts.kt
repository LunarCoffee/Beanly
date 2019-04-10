package framework.extensions

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.exceptions.ErrorResponseException

fun MessageChannel.send(msg: String, after: (Message) -> Unit = {}) {
    sendMessage(msg).queue(after) {
        if (it is ErrorResponseException) {
            error("The message that was supposed to be sent can't fit in a message!")
        }
    }
}

fun MessageChannel.send(embed: MessageEmbed, after: (Message) -> Unit = {}) {
    sendMessage(embed).queue(after) {
        if (it is ErrorResponseException) {
            error("The message that was supposed to be sent can't fit in an embed!")
        }
    }
}

fun MessageChannel.success(msg: String) = send(":white_check_mark:  $msg  **\\o/**")
fun MessageChannel.error(msg: String) = send(":negative_squared_cross_mark:  $msg  **>~<**")
