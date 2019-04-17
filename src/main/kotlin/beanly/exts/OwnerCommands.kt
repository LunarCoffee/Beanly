@file:Suppress("unused")

package beanly.exts

import beanly.exts.utility.ExecResult
import beanly.trimToDescription
import framework.CommandContext
import framework.CommandGroup
import framework.dsl.command
import framework.extensions.await
import framework.extensions.error
import framework.extensions.send
import framework.transformers.TrGreedy
import framework.transformers.TrRest
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import java.io.*
import java.util.concurrent.TimeUnit
import javax.script.ScriptEngineManager
import javax.script.ScriptException
import javax.script.SimpleBindings
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
                .substringAfter(" ")
                .removeSurrounding("```")
                .also { language = it.substringBefore("\n") }
                .substringAfter("\n")
                .split("\n")

            println(language)
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

            result.run {
                """
                |--- $header ---
                |- stderr:$stderr
                |- stdout:$stdout
                |+ Returned `${this.result}` in ~${time}ms."""
                    .trimMargin()
                    .replace(ctx.bot.config.token, "[REDACTED]")
                    .lines()
                    .chunked(20)
                    .forEach { ctx.send("```diff\n${it.joinToString("\n")}```") }
            }
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

        expectedArgs = listOf(TrGreedy(String::toString))
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

            """
            |--- $nameOfExecutor ---
            |- stdout/stderr:$stdoutStderr
            |+ Returned `${process!!.exitValue()}` in ~${time}ms."""
                .trimMargin()
                .replace(ctx.bot.config.token, "[REDACTED]")
                .lines()
                .chunked(20)
                .forEach { ctx.send("```diff\n${it.joinToString("\n")}```") }
        }
    }

    private suspend fun executeKotlin(ctx: CommandContext, codeLines: List<String>): ExecResult {
        val importStatement = """^\s*import\s+([A-z0-9]+\.)*[A-z0-9]+""".toRegex()
        val scriptEngine = ScriptEngineManager()
            .getEngineByName("kotlin")!!
            .also { setIdeaIoUseFallback() }

        // Import statements in the code. These will be prepended to the command prelude.
        val imports = codeLines
            .filter { it.matches(importStatement) }
            .joinToString(";")

        // Remove import statements (the command prelude contains a timing mechanism that runs a
        // lambda, in which you cannot have import statements for some reason).
        val code = codeLines
            .filter { !it.matches(importStatement) }
            .joinToString("\n")

        // Redirect stdout and stderr so we can keep what is outputted.
        val tempStdout = ByteArrayOutputStream().also { System.setOut(PrintStream(it)) }
        val tempStderr = ByteArrayOutputStream().also { System.setErr(PrintStream(it)) }

        val (time, result) = try {
            withTimeout(60_000) {
                scriptEngine.eval(
                    // This prepends imports and adds the actual code.
                    File("src/main/resources/ek_prelude.txt").readText().format(imports, code),
                    SimpleBindings().apply { put("ctx", ctx) }
                ) as Pair<*, *>
            }
        } catch (e: ScriptException) {
            ctx.error("Error during execution! Check your PMs for details.")
            ctx.event.author.openPrivateChannel().await().send(e.toString())
            return ExecResult.ERROR
        } catch (e: TimeoutCancellationException) {
            ctx.error("Execution took too long!")
            return ExecResult.ERROR
        } finally {
            // Reset stdout and stderr.
            System.setOut(PrintStream(FileOutputStream(FileDescriptor.out)))
            System.setErr(PrintStream(FileOutputStream(FileDescriptor.err)))
        }

        val stdout = if (tempStdout.size() > 0) "\n${tempStdout.toString().trim()}" else ""
        val stderr = if (tempStderr.size() > 0) "\n${tempStderr.toString().trim()}" else ""

        return ExecResult(
            "JSR223 Kotlin Scripting Engine (1.3.21)",
            stdout,
            stderr,
            result,
            time as Long
        )
    }
}
