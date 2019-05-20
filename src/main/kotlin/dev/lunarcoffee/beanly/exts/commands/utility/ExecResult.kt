package dev.lunarcoffee.beanly.exts.commands.utility

import dev.lunarcoffee.framework.api.extensions.await
import dev.lunarcoffee.framework.api.extensions.error
import dev.lunarcoffee.framework.api.extensions.send
import dev.lunarcoffee.framework.core.CommandContext
import kotlinx.coroutines.*
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import java.io.*
import java.util.concurrent.TimeUnit
import javax.script.ScriptEngineManager
import javax.script.ScriptException
import javax.script.SimpleBindings
import kotlin.system.measureNanoTime

private val importStatement = """^\s*import\s+([A-z0-9]+\.)*[A-z0-9]+""".toRegex()
private val scriptEngine = ScriptEngineManager()
    .getEngineByName("kotlin")!!
    .also { setIdeaIoUseFallback() }

private const val shellScriptRoot = "src/main/resources/sh"

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

suspend fun executeShellScript(ctx: CommandContext, script: String): ExecResult {
    File("$shellScriptRoot/ex.sh").writeText("#!/bin/bash\n$script")

    // Can't leave this uninitialized, maybe contracts will help in the future?
    var process: Process? = null
    val fileOut = File("$shellScriptRoot/out.txt")
    val fileErr = File("$shellScriptRoot/err.txt")

    val time = measureNanoTime {
        try {
            process = ProcessBuilder("bash", "$shellScriptRoot/ex.sh")
                .redirectOutput(ProcessBuilder.Redirect.to(fileOut))
                .redirectError(ProcessBuilder.Redirect.to(fileErr))
                .start()
                .apply { waitFor(30, TimeUnit.SECONDS) }
        } catch (e: IOException) {
            GlobalScope.launch {
                ctx.error("Error starting process! Check your PMs for details.")
                ctx.event.author.openPrivateChannel().await().send(e.toString())
            }
            return ExecResult.ERROR
        }
    } / 1_000_000

    // Get correct shell environment name based on OS.
    val osName = System.getProperty("os.name")
    val nameOfExecutor = when {
        "Windows" in osName -> "Windows PowerShell 6.1"
        "Linux" in osName -> "GNU Bash 4.4.19"
        else -> "Unknown Shell Environment"
    }

    val stdout = "\n${fileOut.readText().trim()}".ifBlank { "" }
    val stderr = "\n${fileErr.readText().trim()}".ifBlank { "" }

    return ExecResult(
        nameOfExecutor,
        stdout,
        stderr,
        process!!.exitValue(),
        time
    )
}
