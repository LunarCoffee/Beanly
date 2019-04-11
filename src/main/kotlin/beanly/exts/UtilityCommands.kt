@file:Suppress("unused")

package beanly.exts

import beanly.consts.EMOJI_PAGE_FACING_UP
import beanly.ifEmptyToString
import beanly.trimToDescription
import framework.CommandGroup
import framework.dsl.command
import framework.dsl.embed
import framework.extensions.error
import framework.extensions.send
import framework.extensions.success
import framework.transformers.TrSplit
import framework.transformers.TrWord
import java.util.*
import kotlin.math.pow
import kotlin.math.roundToInt

@CommandGroup("Utility")
class UtilityCommands {
    fun rpn() = command("rpn") {
        description = "Reverse polish notation calculator! I'm not sure why this exists."
        aliases = listOf("reversepolish")

        extDescription = """
            |`rpn expression`\n
            |Calculates the result of a expression in reverse Polish notation (postfix notation).
            |The supported operators are: [`+`, `-`, `*`, `/`, `**`, `%`, `&`, `|`, `^`]
        """.trimToDescription()

        expectedArgs = listOf(TrSplit(name = "expression"))
        execute { ctx, args ->
            val expression = args.get<List<String>>(0)
            val stack = Stack<Double>()
            val operators = listOf("+", "-", "*", "/", "**", "%", "&", "|", "^")

            for (token in expression) {
                val number = token.toDoubleOrNull()
                if (number != null) {
                    stack.push(number)
                    continue
                }

                if (token !in operators) {
                    ctx.error("Something was wrong with your expression!")
                    return@execute
                }

                val (op1, op2) = try {
                    Pair(stack.pop(), stack.pop())
                } catch (e: EmptyStackException) {
                    ctx.error("Something was wrong with your expression!")
                    return@execute
                }

                stack.push(
                    when (token) {
                        "+" -> op1 + op2
                        "-" -> op1 - op2
                        "*" -> op1 * op2
                        "/" -> op1 / op2
                        "**" -> op1.pow(op2)
                        "%" -> op1 % op2
                        else -> {
                            val o1 = op1.roundToInt()
                            val o2 = op2.roundToInt()

                            when (token) {
                                "&" -> o1 and o2
                                "|" -> o1 or o2
                                "^" -> o1 xor o2
                                else -> throw IllegalStateException()
                            }.toDouble()
                        }
                    }
                )
            }

            try {
                ctx.success("The result of the calculation is ${stack.pop()}!")
            } catch (e: EmptyStackException) {
                ctx.error("Something was wrong with your expression!")
            }
        }
    }

    fun help() = command("help") {
        description = "Shows help text. Type `..help help -v` for more info."
        extDescription = """
            |`help [command name] [-v]`\n
            |With a command name, this command gets its aliases, short description, expected
            |arguments, and optionally (if the `-v` flag is set) an extended description (which
            |you're reading right now). Otherwise, this command simply lists available commands.
        """.trimToDescription()

        expectedArgs = listOf(TrWord(true, name = "command name"), TrWord(true, name = "flags"))
        execute { ctx, args ->
            val commandName = args.get<String>(0)
            val flags = args.get<String>(1)
            val command = ctx.bot.commands.find { commandName in it.names }

            if (!commandName.isBlank() && command == null) {
                ctx.error("I can't find that command!")
                return@execute
            }

            ctx.send(
                embed {
                    if (commandName.isBlank()) {
                        title = "$EMOJI_PAGE_FACING_UP  All commands:"

                        for ((group, commands) in ctx.bot.groupToCommands) {
                            val names = commands.map { it.name }
                            description += "**${group.name}**: $names\n"
                        }
                        description += "Type `..help help` for more info."
                    } else if (command != null) {
                        title = "$EMOJI_PAGE_FACING_UP  Info on **${command.name}**:"
                        description = """
                            |**Aliases**: ${command.aliases.ifEmptyToString()}
                            |**Description**: ${command.description}
                            |**Arguments**: ${command.expectedArgs.ifEmptyToString()}
                        """.trimMargin()

                        if ("v" in flags) {
                            description += "\n**Extended description**: ${command.extDescription}"
                        }
                    }
                }
            )
        }
    }
}
