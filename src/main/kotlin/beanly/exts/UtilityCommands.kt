@file:Suppress("unused")

package beanly.exts

import beanly.consts.EMOJI_PAGE_FACING_UP
import beanly.ifEmptyToString
import framework.CommandGroup
import framework.dsl.command
import framework.dsl.embed
import framework.extensions.error
import framework.extensions.send
import framework.extensions.success
import framework.transformers.TrSplit
import framework.transformers.TrWord
import java.util.*

@CommandGroup("Utility")
class UtilityCommands {
    fun rpn() = command("rpn") {
        description = "Reverse polish notation calculator! ...Not sure why this exists."
        aliases = listOf("reversepolish")

        expectedArgs = listOf(TrSplit(name = "expression"))
        execute { ctx, args ->
            val expression = args.get<List<String>>(0)
            val stack = Stack<Int>()

            for (token in expression) {
                val number = token.toIntOrNull()
                if (number != null) {
                    stack.push(number)
                    continue
                }

                when (token) {
                    "+" -> stack.push(stack.pop() + stack.pop())
                    "-" -> stack.push(-stack.pop() + stack.pop())
                    "*" -> stack.push(stack.pop() * stack.pop())
                    "/" -> {
                        val o = stack.pop()
                        stack.push(stack.pop() / o)
                    }
                }
            }

            ctx.success("The result of the calculation is ${stack.pop()}!")
        }
    }

    fun help() = command("help") {
        description = "Shows help text for all or a specific command."

        expectedArgs = listOf(TrWord(true, name = "command name"))
        execute { ctx, args ->
            val commandName = args.get<String>(0)
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
                    } else if (command != null) {
                        title = "$EMOJI_PAGE_FACING_UP  Info on **${command.name}**:"
                        description = """
                            **Aliases**: ${command.aliases.ifEmptyToString()}
                            **Description**: ${command.description}
                            **Arguments**: ${command.expectedArgs.ifEmptyToString()}
                        """.trimIndent()
                    }
                }
            )
        }
    }
}
