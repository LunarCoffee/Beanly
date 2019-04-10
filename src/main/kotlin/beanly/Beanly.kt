package beanly

import framework.dsl.startBot
import net.dv8tion.jda.api.entities.Activity

fun main() {
    startBot("src/main/resources/config.yaml") {
        activity = Activity.watching("for ..help.")
    }
}
