package dev.lunarcoffee.beanly

import dev.lunarcoffee.framework.api.dsl.startBot
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity

fun main() {
    startBot("src/main/resources/config.yaml") {
        activity = Activity.watching("for ..help.")
        status = OnlineStatus.DO_NOT_DISTURB
    }
}
