@file:Suppress("unused")

package beanly.exts.commands

import beanly.consts.DB
import beanly.consts.Emoji
import beanly.consts.TIME_FORMATTER
import beanly.exts.commands.utility.RemindTimer
import beanly.gmtToEst
import beanly.ifEmptyToString
import beanly.trimToDescription
import framework.api.dsl.command
import framework.api.dsl.embed
import framework.api.extensions.error
import framework.api.extensions.send
import framework.api.extensions.success
import framework.core.annotations.CommandGroup
import framework.core.transformers.*
import framework.core.transformers.utility.SplitTime
import framework.core.transformers.utility.UserNotFound
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.entities.User
import org.litote.kmongo.coroutine.insertOne
import java.time.Instant
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.pow
import kotlin.math.roundToInt

@CommandGroup("Utility")
class UtilityCommands {
    fun ui() = command("ui") {
        description = "Gets information about a user."
        aliases = listOf("userinfo")

        extDescription = """
            |`$name [user]`\n
            |Gets basic information about a user. If a name or ID is specified, this command will
            |attempt to fetch a user with them. If not, the author of the message will be used. If
            |the user is in the current server, the `mi` command may provide more detailed info.
        """.trimToDescription()

        expectedArgs = listOf(TrUser(true))
        execute { ctx, args ->
            if (args.get<User>(0) is UserNotFound) {
                ctx.error("I can't find that user!")
                return@execute
            }

            val user = args[0] ?: ctx.event.author
            val botOrUser = if (user.isBot) "bot" else "user"

            ctx.send(
                embed {
                    user.run {
                        title = "${Emoji.MAG_GLASS}  Info on $botOrUser **$asTag**:"
                        description = """
                            |**User ID**: $id
                            |**Creation time**: ${timeCreated.gmtToEst().format(TIME_FORMATTER)}
                            |**Avatar ID**: ${avatarId ?: "(none)"}
                            |**Mention**: $asMention
                        """.trimMargin()

                        thumbnail {
                            url = avatarUrl ?: defaultAvatarUrl
                        }
                    }
                }
            )
        }
    }

    fun mi() = command("mi") {
        description = "Gets information about a member of the current server."
        aliases = listOf("memberinfo")

        extDescription = """
            |`$name [user]`\n
            |Gets detailed information about a member of the current server. If a name or ID is
            |specified, this command will attempt to fetch a user with them. If not, the author of
            |the message will be used. If the user is not in the current server, the `ui` command
            |may be useful.
        """.trimToDescription()

        expectedArgs = listOf(TrUser(true))
        execute { ctx, args ->
            if (args.get<User>(0) is UserNotFound) {
                ctx.error("I can't find that user!")
                return@execute
            }

            val member = ctx.event.guild.getMember(args[0] ?: ctx.event.author)

            if (member == null) {
                ctx.error("That user is not a member of this server!")
                return@execute
            }

            ctx.send(
                member.run {
                    val botOrMember = if (user.isBot) "bot" else "member"
                    val activity = activities.firstOrNull()?.name ?: "(none)"

                    val userRoles = if (roles.isNotEmpty()) {
                        "[${roles.joinToString { it.asMention }}]"
                    } else {
                        "(none)"
                    }

                    embed {
                        title = "${Emoji.MAG_GLASS}  Info on $botOrMember **${user.asTag}**:"
                        description = """
                            |**User ID**: $id
                            |**Nickname**: ${nickname ?: "(none)"}
                            |**Status**: ${onlineStatus.key}
                            |**Activity**: $activity
                            |**Creation time**: ${timeCreated.gmtToEst().format(TIME_FORMATTER)}
                            |**Join time**: ${timeJoined.gmtToEst().format(TIME_FORMATTER)}
                            |**Avatar ID**: ${user.avatarId ?: "(none)"}
                            |**Mention**: $asMention
                            |**Roles**: $userRoles
                        """.trimMargin()

                        thumbnail {
                            url = user.avatarUrl ?: user.defaultAvatarUrl
                        }
                    }
                }
            )
        }
    }

    fun rpn() = command("rpn") {
        description = "Reverse polish notation calculator! I'm not sure why this exists."
        aliases = listOf("reversepolish")

        extDescription = """
            |`$name expression`\n
            |Calculates the result of a expression in reverse Polish notation (postfix notation).
            |The supported operators are: [`+`, `-`, `*`, `/`, `**`, `%`, `&`, `|`, `^`]
        """.trimToDescription()

        expectedArgs = listOf(TrSplit())
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

                val (op2, op1) = try {
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
                ctx.success("The result of the calculation is `${stack.pop()}`!")
            } catch (e: EmptyStackException) {
                ctx.error("Something was wrong with your expression!")
            }
        }
    }

    fun remind() = command("remind") {
        val reminderCol = DB.getCollection<RemindTimer>("RemindTimers")

        description = "Sets a reminder so you don't have to remember things!"
        aliases = listOf("reminder")

        extDescription = """
            |`$name time [reason]`\n
            |This command takes a time string that looks something like `3h 40m` or `1m 30s` or
            |`2d 4h 32m 58s`, and optionally, a reason to remind you of. After the amount of time
            |specified in `time`, I will ping you in the channel you send the command in and remind
            |you of what you told me to.
        """.trimToDescription()

        expectedArgs = listOf(TrTime(), TrRest(true, "(no reason)"))
        execute { ctx, args ->
            val time = args.get<SplitTime>(0)
            val reason = args.get<String>(1)

            val dateTime = time.asLocal.format(TIME_FORMATTER).replace(" at ", "` at `").drop(4)
            ctx.success("I'll remind you on `$dateTime`!")

            // Save in DB for reload on bot relaunch.
            val reminderDate = RemindTimer(
                Date.from(Instant.now().plusMillis(time.totalMs)),
                ctx.guild.id,
                ctx.event.channel.id,
                ctx.event.author.asMention,
                reason
            )
            reminderCol.insertOne(reminderDate)

            reminderDate.run {
                Timer().schedule(this@run.time) {
                    GlobalScope.launch {
                        ctx.jda.getGuildById(guildId)!!.getTextChannelById(channelId)!!.success(
                            "Hey, $mention! Here's your reminder: `${this@run.reason}`"
                        )
                    }
                }
            }
        }
    }

    fun help() = command("help") {
        description = "Lists all commands or shows help for a specific command."
        extDescription = """
            |`$name [command name] [-v]`\n
            |With a command name, this command gets its aliases, expected usage, expected
            |arguments, and optionally (if the `-v` flag is set) an extended description (which
            |you're reading right now). Otherwise, this command simply lists available commands.
            |The syntax of the expected usage is as follows:\n
            | - `name`: denotes that `name` is required\n
            | - `name1|name2`: denotes that either `name1` or `name2` is valid\n
            | - `name...`: denotes that many of `name` can be specified\n
            |If an argument is wrapped with square brackets, it is optional.
        """.trimToDescription()

        expectedArgs = listOf(TrWord(true), TrWord(true))
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
                        title = "${Emoji.PAGE_FACING_UP}  All commands:"

                        for (c in ctx.bot.commands.distinctBy { it.groupName }) {
                            // Hide owner-only commands unless the command user is the owner.
                            if (c.groupName != "Owner" || ctx.isOwner()) {
                                val names = ctx
                                    .bot
                                    .commands
                                    .filter { it.groupName == c.groupName }
                                    .map { it.name }
                                description += "**${c.groupName}**: $names\n"
                            }
                        }

                        footer {
                            text = "Type '..help help' for more info."
                        }
                    } else if (command != null) {
                        // The first line of the extended description should always be the command
                        // usage (i.e. `help [command name] [-v]`).
                        val usage = command.extDescription.substringBefore("\n")

                        title = "${Emoji.PAGE_FACING_UP}  Info on **${command.name}**:"
                        description = """
                            |**Aliases**: ${command.aliases.ifEmptyToString()}
                            |**Description**: ${command.description}
                        """.trimMargin()

                        if (flags == "-v") {
                            description += "\n**Extended description**: ${command.extDescription}"
                        } else {
                            description += "\n**Usage**: $usage"
                            footer {
                                text = "Type '..help ${command.name} -v' for more info."
                            }
                        }
                    }
                }
            )
        }
    }
}
