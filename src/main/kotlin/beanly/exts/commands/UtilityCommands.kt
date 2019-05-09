@file:Suppress("unused")

package beanly.exts.commands

import beanly.consts.*
import beanly.exts.commands.utility.timers.RemindTimer
import beanly.gmtToEst
import beanly.trimToDescription
import framework.api.dsl.command
import framework.api.dsl.embed
import framework.api.dsl.embedPaginator
import framework.api.extensions.error
import framework.api.extensions.send
import framework.api.extensions.success
import framework.core.annotations.CommandGroup
import framework.core.transformers.*
import framework.core.transformers.utility.Found
import framework.core.transformers.utility.SplitTime
import framework.core.transformers.utility.UserSearchResult
import org.litote.kmongo.eq
import java.time.Instant
import java.util.*
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.reflect.jvm.jvmName

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
            val user = when (val result = args.get<UserSearchResult?>(0)) {
                is Found -> result.user
                null -> ctx.event.author
                else -> {
                    ctx.error("I can't find that user!")
                    return@execute
                }
            }

            ctx.send(
                embed {
                    user.run {
                        val botOrUser = if (isBot) "bot" else "user"

                        title = "${Emoji.MAG_GLASS}  Info on $botOrUser **$asTag**:"
                        description = """
                            |**User ID**: $id
                            |**Creation time**: ${timeCreated.gmtToEst().format(TIME_FORMATTER)}
                            |**Avatar ID**: ${avatarId ?: "(none)"}
                            |**Mention**: $asMention
                        """.trimMargin()

                        thumbnail { url = avatarUrl ?: defaultAvatarUrl }
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
            val searchResult = args.get<UserSearchResult?>(0)
            val member = when (val result = searchResult ?: Found(ctx.event.author)) {
                is Found -> ctx.event.guild.getMember(result.user)
                else -> {
                    ctx.error("I can't find that user!")
                    return@execute
                }
            }

            if (member == null) {
                ctx.error("That user is not a member of this server!")
                return@execute
            }

            ctx.send(
                embed {
                    member.run {
                        val botOrMember = if (user.isBot) "bot" else "member"
                        val activity = activities.firstOrNull()?.name ?: "(none)"

                        val userRoles = if (roles.isNotEmpty()) {
                            "[${roles.joinToString { it.asMention }}]"
                        } else {
                            "(none)"
                        }

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

                        thumbnail { url = user.avatarUrl ?: user.defaultAvatarUrl }
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
        val reminderCol = DB.getCollection<RemindTimer>(COL_NAMES[RemindTimer::class.simpleName]!!)

        description = "Sets a reminder so you don't have to remember things!"
        aliases = listOf("remindme")

        extDescription = """
            |`$name time [reason]`\n
            |This command takes a time string that looks something like `3h 40m` or `1m 30s` or
            |`2d 4h 32m 58s`, and optionally, a reason to remind you of. After the amount of time
            |specified in `time`, I will ping you in the channel you send the command in and remind
            |you of what you told me to. Also, if your reminder doesn't fire and you get promoted
            |to a customer or your house burns down, it isn't my fault.
        """.trimToDescription()

        expectedArgs = listOf(TrTime(), TrRest(true, "(no reason)"))
        execute { ctx, args ->
            val time = args.get<SplitTime>(0)
            val reason = args.get<String>(1)

            val dateTime = time.asLocal.format(TIME_FORMATTER).replace(" at ", "` at `").drop(4)
            ctx.success("I'll remind you on `$dateTime`!")

            val reminderTimer = RemindTimer(
                Date.from(Instant.now().plusMillis(time.totalMs)),
                ctx.guild.id,
                ctx.event.channel.id,
                ctx.event.author.asMention,
                reason
            )

            // Save in DB for reload on bot relaunch.
            reminderCol.insertOne(reminderTimer)
            reminderTimer.schedule(ctx.event, reminderCol)
        }
    }

    fun remindlist() = command("remindlist") {
        val reminderCol = DB.getCollection<RemindTimer>(COL_NAMES[RemindTimer::class.simpleName]!!)

        description = "Lets you view and cancel your reminders."
        aliases = listOf("remindmanage")

        extDescription = """
            |`$name [list|cancel] [id|range]`\n
            |This command can list all of your active reminders or cancel one or more of said
            |reminders. Using the command without arguments will list active reminders, and using
            |it with two will cancel one. When cancelling, the first argument needs to be `cancel`
            |and the second needs to be either a number or inclusive range (like `1-3` or `4-5`)
            |that includes the numbers of the reminders to be cancelled (which you can see by using
            |this command without arguments).
        """.trimToDescription()

        expectedArgs = listOf(TrWord(true, "list"), TrWord(true))
        execute { ctx, args ->
            val operation = args.get<String>(0)
            val idOrRange = args.get<String>(1)

            // This command lets users remove either a single reminder or reminders within a range
            // of IDs. This here tries to use the input as a range first, then as a single number.
            val potentialId = idOrRange.toIntOrNull()
            val range = TrIntRange(true, 0..0).transform(ctx.event, mutableListOf(idOrRange)).run {
                when {
                    this == 0..0 && potentialId != null -> potentialId..potentialId
                    this == 0..0 && operation != "list" -> {
                        ctx.error("That isn't a valid number or range!")
                        return@execute
                    }
                    else -> this
                }
            }
            val rangeIsMoreThanOne = range.count() > 1

            if (operation == "list") {
                val list = reminderCol
                    .find(RemindTimer::mention eq ctx.event.author.asMention)
                    .toList()

                if (list.isEmpty()) {
                    ctx.success("You have no reminders!")
                    return@execute
                }

                val reminderPages = list.mapIndexed { i, reminder ->
                    val time = SplitTime(reminder.time.time - Date().time)
                        .asLocal
                        .format(TIME_FORMATTER)
                        .drop(4)

                    "**#${i + 1}**: `${reminder.reason.replace("`", "")}` on $time"
                }.chunked(16).map { it.joinToString("\n") }

                ctx.send(
                    embedPaginator(ctx.event.author) {
                        for (reminders in reminderPages) {
                            page(
                                embed {
                                    title = "${Emoji.ALARM_CLOCK}  Your reminders:"
                                    description = reminders
                                }
                            )
                        }
                    }
                )
            } else if (operation == "cancel") {
                val reminders = reminderCol
                    .find(RemindTimer::mention eq ctx.event.author.asMention)
                    .toList()

                // Check that the reminder number or range exists.
                if (reminders.size + 1 in range) {
                    ctx.error(
                        if (rangeIsMoreThanOne) {
                            "Some of those reminders don't exist!"
                        } else {
                            "A reminder with that number doesn't exist!"
                        }
                    )
                    return@execute
                }

                // Can't seem to use [deleteMany] since we need to check indices.
                for (index in range) {
                    reminderCol.deleteOne(reminders[index - 1].isSame())
                }

                val pluralThat = if (rangeIsMoreThanOne) "those reminders" else "that reminder"
                ctx.success("I've removed $pluralThat!")
            }
        }
    }

    fun rev() = command("rev") {
        description = "Reverses the given text."
        aliases = listOf("reverse", "backwards")

        extDescription = """
            |`$name text [-w]`\n
            |Reverses the given text, letter by letter if the `-w` flag is not specified, and word
            |by word if it is specified (the text is simply split by spaces).
        """.trimToDescription()

        expectedArgs = listOf(TrRest())
        execute { ctx, args ->
            val rawText = args.get<String>(0)

            val byWords = rawText.endsWith(" -w")
            val text = if (byWords) {
                rawText.split(" ").dropLast(1).reversed().joinToString(" ")
            } else {
                rawText.reversed()
            }

            ctx.success("Your text reversed is `$text`")
        }
    }

    fun len() = command("len") {
        description = "Shows the length of the given text."
        aliases = listOf("length")

        extDescription = """
            |`$name text [-w]`\n
            |Counts the characters in the given text if the `-w` flag is not specified, and counts
            |words if it is specified (the text is simply split by spaces).
        """.trimToDescription()

        expectedArgs = listOf(TrRest())
        execute { ctx, args ->
            val rawText = args.get<String>(0)
            val byWords = rawText.endsWith(" -w")

            val length = if (byWords) rawText.split(" ").size - 1 else rawText.length
            val charsOrWords = if (byWords) "words" else "characters"

            ctx.success("Your text is `$length` $charsOrWords long.")
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
            |If an argument is wrapped with square brackets, it is optional. You may wrap an
            |argument with double quotes "like this" to treat it as one instead of multiple.
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

                        footer { text = "Type '..help help' for more info." }
                    } else if (command != null) {
                        // The first line of the extended description should always be the command
                        // usage (i.e. `help [command name] [-v]`).
                        val usage = command.extDescription.substringBefore("\n")
                        val aliases = if (command.aliases.count() == 0) {
                            "(none)"
                        } else {
                            command.aliases.toString()
                        }

                        title = "${Emoji.PAGE_FACING_UP}  Info on **${command.name}**:"
                        description = """
                            |**Aliases**: $aliases
                            |**Description**: ${command.description}
                        """.trimMargin()

                        if (flags == "-v") {
                            description += "\n**Extended description**: ${command.extDescription}"
                        } else {
                            description += "\n**Usage**: $usage"
                            footer { text = "Type '..help ${command.name} -v' for more info." }
                        }
                    }
                }
            )
        }
    }
}
