@file:Suppress("unused")

package beanly.groups

import beanly.consts.EMBED_COLOR
import beanly.consts.EMOJI_OPEN_FILE_FOLDER
import beanly.consts.EMOJI_PING_PONG
import framework.CommandGroup
import framework.dsl.command
import framework.dsl.embed
import framework.extensions.await
import framework.extensions.send
import framework.extensions.success
import framework.transformers.TrInt
import java.io.File
import kotlin.random.Random

@CommandGroup("Misc")
class MiscCommands {
    fun ping() = command("ping") {
        description = "Gets gateway and API latencies."
        aliases = listOf("pong", "peng")

        execute { ctx, _ ->
            val ping = ctx.jda.restPing.await()

            ctx.send(
                embed {
                    setTitle("$EMOJI_PING_PONG  Pong!")
                    setDescription("[${ctx.jda.gatewayPing}ms, ${ping}ms]")
                    setColor(EMBED_COLOR)
                }
            )
        }
    }

    fun loc() = command("loc") {
        description = "Gets various stats about my code! :3"
        aliases = listOf("linesofcode")

        execute { ctx, _ ->
            val files = File("src/main").walk().filter { it.extension == "kt" }
            val linesOfCode = files.sumBy { it.readLines().count() }

            ctx.send(
                embed {
                    setTitle("$EMOJI_OPEN_FILE_FOLDER  Code statistics:")
                    setDescription(
                        """
                        **Code files**: ${files.count()}
                        **Lines of code**: $linesOfCode
                        """.trimIndent()
                    )
                    setColor(EMBED_COLOR)
                }
            )
        }
    }

    fun rng() = command("rng") {
        description = "Gets you a random number between two numbers (inclusive)."
        aliases = listOf("rand", "random")

        expectedArgs = listOf(TrInt(name = "lower limit"), TrInt(name = "upper limit"))
        execute { ctx, args ->
            val lowerBound = args.get<Int>(0)
            val upperBound = args.get<Int>(1)

            ctx.success("Your random number is ${Random.nextInt(lowerBound, upperBound)}!")
        }
    }

    fun git() = command("git") {
        description = "Gets my GitLab repo URL."
        aliases = listOf("repo", "gitlab")

        execute { ctx, _ ->
            ctx.success("<https://gitlab.com/LunarCoffee/beanly>")
        }
    }
}
