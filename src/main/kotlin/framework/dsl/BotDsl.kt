package framework.dsl

import framework.Bot
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.jetbrains.kotlin.resolve.lazy.descriptors.script.classId
import kotlin.reflect.KClass

class BotDsl(configPath: String) : Bot(configPath) {
    var activity: Activity?
        get() = jda.presence.activity
        set(value) {
            jda.presence.activity = value
        }

    var listenerClass: KClass<out ListenerAdapter>? = null
        set(value) {
            jda.addEventListener(
                value
                    ?.constructors
                    ?.find {
                        // Make sure the constructor takes one argument of type [Bot].
                        it.parameters.run {
                            size == 1 && get(0).type.classId == Bot::class.classId
                        }
                    }
                    ?.call(this)
            )
            field = value
        }
}

fun startBot(configPath: String, init: BotDsl.() -> Unit): Bot {
    return BotDsl(configPath).apply {
        init()
        loadCommands()
    }
}
