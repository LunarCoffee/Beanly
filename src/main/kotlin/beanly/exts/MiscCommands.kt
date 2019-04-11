@file:Suppress("unused")

package beanly.exts

import beanly.consts.EMOJI_OPEN_FILE_FOLDER
import beanly.consts.EMOJI_PING_PONG
import beanly.trimToDescription
import framework.CommandGroup
import framework.dsl.command
import framework.dsl.embed
import framework.extensions.await
import framework.extensions.send
import framework.extensions.success
import framework.transformers.TrInt
import framework.transformers.TrWord
import java.io.File
import java.security.SecureRandom
import kotlin.random.Random
import kotlin.system.measureNanoTime

@CommandGroup("Misc")
class MiscCommands {
    fun ping() = command("ping") {
        description = "Gets gateway and API latencies."
        aliases = listOf("pong", "peng")

        extDescription = """
            |`ping`\n
            |Shows three values that represent the heartbeat, API, and stack allocation latencies.
        """.trimToDescription()

        execute { ctx, _ ->
            val ping = ctx.jda.restPing.await()
            val stackLatency = measureNanoTime {
                @Suppress("UNUSED_VARIABLE")
                val temp = 0
            }

            ctx.send(
                embed {
                    title = "$EMOJI_PING_PONG  Pong!"
                    description = "[${ctx.jda.gatewayPing}ms, ${ping}ms, ${stackLatency}ns]"
                }
            )
        }
    }

    fun loc() = command("loc") {
        description = "Gets various stats about my code!"
        aliases = listOf("linesofcode")

        extDescription = """
            |`loc`\n
            |Shows various project statistics, including line counts, file counts, directory
            |counts, and character counts.
        """.trimToDescription()

        execute { ctx, _ ->
            val files = File("src/main").walk().filter { it.extension == "kt" }
            val dirs = File("src/main").walk().filter { it.isDirectory }.count()

            var linesOfCode = 0
            var blankLines = 0
            var characters = 0

            files
                .flatMap { it.readLines().asSequence() }
                .forEach {
                    if (it.isBlank()) {
                        blankLines++
                    }
                    linesOfCode++
                    characters += it.length
                }

            ctx.send(
                embed {
                    title = "$EMOJI_OPEN_FILE_FOLDER  Code statistics:"
                    description = """
                        **Lines of code**: $linesOfCode
                        **Lines with content**: ${linesOfCode - blankLines}
                        **Blank lines**: $blankLines
                        **Characters**: $characters
                        **Code files**: ${files.count()}
                        **Directories**: $dirs
                    """.trimIndent()
                }
            )
        }
    }

    fun rng() = command("rng") {
        description = "Gets you a random number between two numbers (inclusive)."
        aliases = listOf("rand", "random")

        extDescription = """
            |`rng low high [-s]`\n
            |Generates a random number within [`low`, `high`]. If the `-s` flag is set, a secure
            |source of randomness will be used.
        """.trimToDescription()

        expectedArgs = listOf(TrInt(name = "low"), TrInt(name = "high"), TrWord(true, "flags"))
        execute { ctx, args ->
            val lowerBound = args.get<Int>(0)
            val upperBound = args.get<Int>(1) + 1
            val flags = args.get<String>(2)

            val number = if ("s" in flags) {
                SecureRandom().nextInt(upperBound - lowerBound) + lowerBound
            } else {
                Random.nextInt(lowerBound, upperBound)
            }

            ctx.success("Your random number is $number!")
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
