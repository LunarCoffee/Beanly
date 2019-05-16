package dev.lunarcoffee.beanly.exts.commands.utility

import dev.lunarcoffee.framework.core.CommandContext
import dev.lunarcoffee.framework.api.extensions.await
import dev.lunarcoffee.framework.api.extensions.error
import dev.lunarcoffee.framework.api.extensions.send
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import java.io.*
import javax.script.ScriptEngineManager
import javax.script.ScriptException
import javax.script.SimpleBindings

private val importStatement = """^\s*import\s+([A-z0-9]+\.)*[A-z0-9]+""".toRegex()
private val scriptEngine = ScriptEngineManager()
    .getEngineByName("kotlin")!!
    .also { setIdeaIoUseFallback() }

class ExecResult(
    val header: String,
    val stdout: String,
    val stderr: String,
    val result: Any?,
    val time: Long
) {
    companion object {
        val ERROR = ExecResult("", "", "", null, -1)
    }
}

suspend fun executeKotlin(ctx: CommandContext, codeLines: List<String>): ExecResult {
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
        withContext(Dispatchers.Default) {
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
