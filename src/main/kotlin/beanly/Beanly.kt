package beanly

import framework.dsl.bot
import net.dv8tion.jda.api.entities.Activity

fun main() = bot("..") {
    activity = Activity.watching("for ..help.")
}.start()
