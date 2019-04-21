package framework.dsl

import framework.Bot
import net.dv8tion.jda.api.entities.Activity

class BotDsl(configPath: String) : Bot(configPath) {
    var activity: Activity?
        get() = jda.presence.activity
        set(value) {
            jda.presence.activity = value
        }
}

fun startBot(configPath: String, init: BotDsl.() -> Unit): Bot {
    return BotDsl(configPath).apply {
        init()
        loadCommands()
    }
}
