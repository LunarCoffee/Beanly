@file:Suppress("unused")

package dev.lunarcoffee.beanly.exts.commands

import dev.lunarcoffee.beanly.consts.Emoji
import dev.lunarcoffee.beanly.exts.commands.utility.SystemStatistics
import dev.lunarcoffee.beanly.trimToDescription
import dev.lunarcoffee.framework.api.dsl.command
import dev.lunarcoffee.framework.api.dsl.embed
import dev.lunarcoffee.framework.api.extensions.await
import dev.lunarcoffee.framework.api.extensions.send
import dev.lunarcoffee.framework.api.extensions.success
import dev.lunarcoffee.framework.core.annotations.CommandGroup
import dev.lunarcoffee.framework.core.transformers.TrInt
import dev.lunarcoffee.framework.core.transformers.TrWord
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
            |`$name`\n
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
                    title = "${Emoji.PING_PONG}  Pong!"
                    description = "[${ctx.jda.gatewayPing}ms, ${ping}ms, ${stackLatency}ns]"
                }
            )
        }
    }

    fun loc() = command("loc") {
        description = "Gets various stats about my code!"
        aliases = listOf("linesofcode")

        extDescription = """
            |`$name`\n
            |Shows various project statistics, including line counts, file counts, directory
            |counts, and character counts.
        """.trimToDescription()

        execute { ctx, _ ->
            val files = File("src/main").walk().filter { it.extension == "kt" }
            val dirs = File("src/main").walk().filter { it.isDirectory }.count()

            var linesOfCode = 0
            var blankLines = 0
            var characters = 0

            files.flatMap { it.readLines().asSequence() }.forEach {
                if (it.isBlank()) {
                    blankLines++
                }
                linesOfCode++
                characters += it.length
            }

            ctx.send(
                embed {
                    title = "${Emoji.OPEN_FILE_FOLDER}  Code statistics:"
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
            |`$name low high [-s]`\n
            |Generates a random number within [`low`, `high`]. If the `-s` flag is set, a secure
            |source of randomness will be used.
        """.trimToDescription()

        expectedArgs = listOf(TrInt(), TrInt(), TrWord(true))
        execute { ctx, args ->
            val lowerBound = args.get<Int>(0)
            val upperBound = args.get<Int>(1) + 1
            val flags = args.get<String>(2)

            val number = if (flags == "-s") {
                SecureRandom().nextInt(upperBound - lowerBound) + lowerBound
            } else {
                Random.nextInt(lowerBound, upperBound)
            }

            ctx.success("Your random number is `$number`!")
        }
    }

    fun git() = command("git") {
        description = "Gets my GitHub repo URL."
        aliases = listOf("repo", "github")

        extDescription = """
            |`$name`\n
            |Unless you are a developer, this command probably has no use. My code is licensed
            |under the MIT license.
        """.trimToDescription()

        execute { ctx, _ ->
            ctx.success("<https://github.com/LunarCoffee/Beanly>")
        }
    }

    fun stats() = command("stats") {
        description = "Gets various stats about my... existence?"
        aliases = listOf("statistics")

        extDescription = """
            |`$name`\n
            |Gets various stats about me, like how much RAM I'm eating up, the language I'm written
            |in, how long I've been awake, and what architecture the CPU I'm running on is.
        """.trimToDescription()

        execute { ctx, _ ->
            ctx.send(
                embed {
                    SystemStatistics().run {
                        title = "${Emoji.LAPTOP_COMPUTER}  System statistics:"
                        description = """
                            |**Memory usage**: ${totalMemory - freeMemory}/$totalMemory MB
                            |**Language**: $language
                            |**JVM version**: $jvmVersion
                            |**Operating system**: $osName
                            |**Uptime**: $uptime
                            |**CPU architecture**: $cpuArchitecture
                            |**Logical cores available**: $logicalProcessors
                            |**Total threads**: $totalThreads
                            |**Running threads**: $runningThreads
                        """.trimMargin()
                    }
                }
            )
        }
    }
}
