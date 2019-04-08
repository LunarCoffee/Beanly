package framework.dsl

import framework.Bot
import net.dv8tion.jda.api.JDABuilder
import java.io.File

fun bot(prefix: String, init: Bot.() -> Unit) = Bot(
    JDABuilder()
        .setToken(File("src/main/resources/token.txt").readText())
        .build(),
    prefix
).apply(init)
