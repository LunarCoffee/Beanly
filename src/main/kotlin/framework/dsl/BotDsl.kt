package framework.dsl

import framework.Bot

fun startBot(configPath: String, init: Bot.() -> Unit): Bot {
    return Bot(configPath).apply {
        init()
        start()
    }
}
