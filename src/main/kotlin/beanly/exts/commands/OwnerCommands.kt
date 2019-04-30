@file:Suppress("unused")

package beanly.exts.commands

import beanly.consts.EMBED_COLOR
import beanly.exts.utility.ExecResult
import beanly.exts.utility.executeKotlin
import beanly.trimToDescription
import framework.core.annotations.CommandGroup
import framework.api.dsl.command
import framework.api.dsl.embed
import framework.api.dsl.messagePaginator
import framework.api.extensions.await
import framework.api.extensions.error
import framework.api.extensions.send
import framework.api.extensions.success
import framework.core.transformers.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess
import kotlin.system.measureNanoTime

@CommandGroup("Owner")
class OwnerCommands {
    fun ex() = command("ex") {
        description = "Executes arbitrary code. Only my owner can use this."
        aliases = listOf("exec", "execute")

        ownerOnly = true
        noArgParsing = true

        extDescription = """
            |`$name code`\n
            |Executes Kotlin code in an unconstrained environment. This command can only be used by
            |my owner, for obvious security reasons. The only available global is `ctx`, the
            |`CommandContext` object associated with the current command execution. The event and
            |bot objects can be accessed from the command context.
        """.trimToDescription()

        expectedArgs = listOf(TrRest())
        execute { ctx, args ->
            val code = args.get<String>(0)
            var language: String

            val codeLines = code
                .removeSurrounding("```")
                .also { language = it.substringBefore("\n") }
                .substringAfter("\n")
                .split("\n")

            val result = when (language) {
                "kotlin" -> executeKotlin(ctx, codeLines)
                else -> {
                    ctx.error("You must specify a valid language in a code block!")
                    return@execute
                }
            }

            if (result == ExecResult.ERROR) {
                // No error message is required because the code execution function has already
                // taken care of that.
                return@execute
            }

            ctx.send(
                messagePaginator(ctx.event.author) {
                    result.run {
                        """
                        |--- $header ---
                        |- stderr:$stderr
                        |- stdout:$stdout
                        |+ Returned `${this.result}` in ~${time}ms."""
                            .trimMargin()
                            .replace(ctx.bot.config.token, "[REDACTED]")
                            .lines()
                            .chunked(16)
                            .forEach { page("```diff\n${it.joinToString("\n")}```") }
                    }
                }
            )
        }
    }

    fun sh() = command("sh") {
        description = "Executes a command in a shell."
        aliases = listOf("shell")
        ownerOnly = true

        extDescription = """
            |`$name command`\n
            |Executes a command in an unconstrained bash environment. This command can only be used
            |by my owner, for obvious security reasons.
        """.trimToDescription()

        expectedArgs = listOf(TrSplit())
        execute { ctx, args ->
            val command = args.get<List<String>>(0).toTypedArray()
            var process: Process? = null

            val time = measureNanoTime {
                try {
                    process = ProcessBuilder(*command)
                        .redirectOutput(ProcessBuilder.Redirect.PIPE)
                        .redirectError(ProcessBuilder.Redirect.PIPE)
                        .start()
                        .apply { waitFor(60, TimeUnit.SECONDS) }
                } catch (e: IOException) {
                    GlobalScope.launch {
                        ctx.error("Error starting process! Check your PMs for details.")
                        ctx.event.author.openPrivateChannel().await().send(e.toString())
                    }
                    return@execute
                }
            } / 1_000_000

            // Get correct shell environment name based on OS.
            val osName = System.getProperty("os.name")
            val nameOfExecutor = when {
                "Windows" in osName -> "Windows PowerShell 6.1"
                "Linux" in osName -> "GNU Bash 4.4.19"
                else -> "Unknown Shell Environment"
            }

            // [process] should always be initialized in real use.
            val stdoutStderrText = process!!.inputStream.bufferedReader().readText().trim()
            val stdoutStderr = if (stdoutStderrText.isNotEmpty()) {
                "\n$stdoutStderrText"
            } else {
                ""
            }

            ctx.send(
                messagePaginator(ctx.event.author) {
                    """
                    |--- $nameOfExecutor ---
                    |- stdout/stderr:$stdoutStderr
                    |+ Returned `${process!!.exitValue()}` in ~${time}ms."""
                        .trimMargin()
                        .replace(ctx.bot.config.token, "[REDACTED]")
                        .lines()
                        .chunked(20)
                        .forEach { page("```diff\n${it.joinToString("\n")}```") }
                }
            )
        }
    }

    fun smsg() = command("smsg") {
        description = "Sends a message. Only my owner can use this."
        aliases = listOf("sendmsg")

        ownerOnly = true
        deleteSender = true

        extDescription = """
            |`$name message`\n
            |Sends a message to the command user's channel. This is an owner only command as to
            |prevent spam.
        """.trimToDescription()

        expectedArgs = listOf(TrRest())
        execute { ctx, args ->
            val content = args.get<String>(0)
            ctx.send(content)
        }
    }

    fun semt() = command("semt") {
        description = "Sends one or more emotes. Only my owner can use this."
        aliases = listOf("sendemote")

        ownerOnly = true
        deleteSender = true

        extDescription = """
            |`$name names...`\n
            |Sends one or more emotes to the command user's channel. This is an owner only command
            |for... hm. I'm not too sure why this is an owner only command. I guess you'll have to
            |stick with the `emotes` command. Anyway, if an emote is not found, I simply don't send
            |that one (unlike with `emotes`).
        """.trimToDescription()

        expectedArgs = listOf(TrSplit())
        execute { ctx, args ->
            val emoteNames = args.get<List<String>>(0)
            val emotes = emoteNames
                .mapNotNull { ctx.jda.getEmotesByName(it, true).firstOrNull()?.asMention }
                .joinToString(" ")

            if (emotes.isEmpty()) {
                ctx.error("I don't have any of those emotes!")
                return@execute
            }

            ctx.send(emotes)
        }
    }

    fun sebd() = command("sebd") {
        description = "Sends an embed. Only my owner can use this."
        aliases = listOf("sendembed")

        ownerOnly = true
        deleteSender = true

        extDescription = """
            |`$name message`\n
            |Sends a message embed to the command user's channel. This is an owner only command as
            |to prevent spam. For more advanced usage, it is advised to use the `ex` command.
        """.trimToDescription()

        expectedArgs = listOf(TrWord(), TrWord(), TrInt(true, EMBED_COLOR))
        execute { ctx, args ->
            val titleText = args.get<String>(0)
            val descriptionText = args.get<String>(1)
            val embedColor = args.get<Int>(2)

            ctx.send(
                embed {
                    title = titleText
                    description = descriptionText
                    color = embedColor
                }
            )
        }
    }

    fun shutdown() = command("shutdown") {
        description = "Shuts down the bot. Only my owner can use this."
        ownerOnly = true

        extDescription = """
            |`$name`\n
            |Shuts down the bot process. There is a roughly 5-second long period of time between
            |command usage and actual process termination. This is owner only for obvious reasons.
        """.trimToDescription()

        execute { ctx, _ ->
            ctx.success("Goodbye, world...")

            delay(5000)
            ctx.jda.shutdownNow()
            exitProcess(0)
        }
    }
}
