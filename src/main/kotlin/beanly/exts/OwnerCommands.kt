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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import java.io.*
import java.util.concurrent.TimeUnit
import javax.script.ScriptEngineManager
import javax.script.ScriptException
import javax.script.SimpleBindings
import kotlin.concurrent.thread

@CommandGroup("Owner")
class OwnerCommands {
    fun exec() = command("exec") {
        description = "Executes arbitrary code. Only my owner can use this."
        aliases = listOf("execute")

        ownerOnly = true
        noArgParsing = true

        extDescription = """
            |`exec code`\n
            |Executes Kotlin code in an unconstrained environment. This command can only be used by
            |my owner, for obvious security reasons. The only available global is `ctx`, the
            |`CommandContext` object associated with the current command execution. The event and
            |bot objects can be accessed from the command context.
        """.trimToDescription()

        expectedArgs = listOf(TrRest(name = "code"))
        execute { ctx, args ->
            var language: String
            val codeLines = args
                .get<String>(0)
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

            result.run {
                """
                |--- $header ---
                |- stderr:$stderr
                |- stdout:$stdout
                |+ Returned `${this.result}` in ~${time}ms."""
                    .trimMargin()
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

        expectedArgs = listOf(TrGreedy(String::toString, name = "command"))
        execute { ctx, args ->
            val command = args.get<List<String>>(0).toTypedArray()
            println(command)
            val resultChannel = Channel<String>()

            thread(true, true) {
                val process = try {
                    ProcessBuilder(*command)
                        .redirectOutput(ProcessBuilder.Redirect.PIPE)
                        .redirectError(ProcessBuilder.Redirect.PIPE)
                        .start()
                } catch (e: IOException) {
                    GlobalScope.launch {
                        ctx.error("Error starting process! Check your PMs for details.")
                        ctx.event.author.openPrivateChannel().await().send(e.toString())
                    }
                    return@thread
                }

                process.waitFor(60, TimeUnit.SECONDS)

                val stdoutStderrText = process.inputStream.bufferedReader().readText()
                val stdoutStderr = if (stdoutStderrText.isEmpty()) {
                    "\n$stdoutStderrText"
                } else {
                    ""
                }

                GlobalScope.launch {
                    resultChannel.send(
                        """
                        |--- GNU Bash 4.4.19 ---
                        |- stdout/stderr:$stdoutStderr
                        |+ Returned `0` in ~0ms.
                        """.trimMargin()
                    )
                }
            }

            resultChannel
                .receive()
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
