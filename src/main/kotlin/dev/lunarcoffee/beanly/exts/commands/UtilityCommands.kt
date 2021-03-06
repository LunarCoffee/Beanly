@file:Suppress("unused")

package dev.lunarcoffee.beanly.exts.commands

import dev.lunarcoffee.beanly.consts.COL_NAMES
import dev.lunarcoffee.beanly.consts.DB
import dev.lunarcoffee.beanly.consts.Emoji
import dev.lunarcoffee.beanly.exts.commands.utility.FastFactorialCalculator
import dev.lunarcoffee.beanly.exts.commands.utility.tags.TagManager
import dev.lunarcoffee.beanly.exts.commands.utility.timers.RemindTimer
import dev.lunarcoffee.beanly.trimToDescription
import dev.lunarcoffee.framework.api.dsl.command
import dev.lunarcoffee.framework.api.dsl.embed
import dev.lunarcoffee.framework.api.dsl.embedPaginator
import dev.lunarcoffee.framework.api.dsl.messagePaginator
import dev.lunarcoffee.framework.api.extensions.error
import dev.lunarcoffee.framework.api.extensions.send
import dev.lunarcoffee.framework.api.extensions.success
import dev.lunarcoffee.framework.core.annotations.CommandGroup
import dev.lunarcoffee.framework.core.transformers.*
import dev.lunarcoffee.framework.core.transformers.utility.SplitTime
import org.litote.kmongo.eq
import java.time.Instant
import java.util.*
import kotlin.math.pow
import kotlin.math.roundToInt

@CommandGroup("Utility")
class UtilityCommands {
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
            |specified in `time`, I should ping you in the channel you send the command in and
            |remind you of what you told me.
        """.trimToDescription()

        expectedArgs = listOf(TrTime(), TrRest(true, "(no reason)"))
        execute { ctx, args ->
            val time = args.get<SplitTime>(0)
            val reason = args.get<String>(1)

            val dateTime = time.localWithoutWeekday().replace(" at ", "` at `")
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
            |`$name [cancel] [id|range]`\n
            |This command is for managing reminders made by the `remind` command. You can view and
            |cancel any of your reminders here.
            |&{Viewing reminders:}
            |Seeing your active reminders is easy. Just use the command without arguments (i.e.
            |`..remindlist`), and I will list out all of your active reminders. Each entry will
            |have the reminder's reason and the time it will be fired at.
            |&{Cancelling reminders:}
            |Reminder cancellation is also easy. The first argument must be `cancel`, and the
            |second argument can be either a number or range of numbers (i.e. `1-5` or `4-6`). I
            |will cancel the reminders with the IDs you specify (either `id` or `range`).
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
                    val time = SplitTime(reminder.time.time - Date().time).localWithoutWeekday()

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

    fun fact() = command("fact") {
        description = "Calculates the factorial of a given number."
        aliases = listOf("factorial")

        extDescription = """
            |`$name number`\n
            |A lot of online calculators stop giving you factorials in whole numbers after quite an
            |early point, usually around `15!` or so. Unlike them, I'll calculate factorials up to
            |50000 and happily provide them in all their glory.
        """.trimToDescription()

        expectedArgs = listOf(TrInt())
        execute { ctx, args ->
            val number = args.get<Int>(0).toLong()
            if (number !in 0..50_000) {
                ctx.error("I can't calculate the factorial of that number!")
                return@execute
            }
            val result = FastFactorialCalculator.factorial(number).toString().chunked(1_827)

            ctx.send(
                messagePaginator(ctx.event.author) {
                    for (chunk in result) {
                        page("```$chunk```")
                    }
                }
            )
        }
    }

    fun tags() = command("tags") {
        description = "Create textual tags to save!"
        aliases = listOf("notes")

        extDescription = """
            |`$name [action] [tag name] [tag content]`\n
            |This command lets you save information in tags. These tags have a name and can store
            |a lot of content. They also store the person who created them and the time at which
            |the tag was created.
            |&{Viewing tags:}
            |To view all the tags on the current server, use the command with no arguments (as in
            |`..$name` only). To view a specific tag, action should be `view` and `tag name` should
            |be the name of the tag you want to view.
            |&{Adding tags:}
            |To add a tag, `action` should be `add`, `tag name` should be the name you want to
            |give the tag, and `tag content` should be the content of the tag. The name cannot be
            |longer than 30 characters, and the content cannot be longer than 1000 characters.
            |&{Editing tags:}
            |If you ever want to change one of your tags, you can do so by making `action` the word
            |`edit`, `tag name` the name of the tag you want to edit (you have to have created it),
            |and `tag content` the updated content of the tag.
            |&{Deleting tags:}
            |To remove a tag, `action` has to be `delete`, and `tag name` has to be the name of the
            |tag you want to delete (which has to have been created by you).
        """.trimToDescription()

        expectedArgs = listOf(TrWord(true), TrWord(true), TrRest(true))
        execute { ctx, args ->
            val action = args.get<String>(0)
            if (action.isEmpty()) {
                TagManager.sendTags(ctx)
                return@execute
            }

            val tagName = args.get<String>(1)
            val tagContent = args.get<String>(2)

            TagManager.run {
                when (action) {
                    "view" -> sendOneTag(ctx, tagName)
                    "add" -> addTag(ctx, tagName, tagContent)
                    "edit" -> editTag(ctx, tagName, tagContent)
                    "delete" -> deleteTag(ctx, tagName)
                    else -> {
                        ctx.error("That operation is invalid!")
                    }
                }
            }
        }
    }

    fun help() = command("help") {
        val singleField = """&\{([^{}]+)}""".toRegex()

        description = "Lists all commands or shows help for a specific command."
        extDescription = """
            |`$name [command name] [-v]`\n
            |With a command name, this command gets its aliases, expected usage, expected
            |arguments, and optionally (if the `-v` flag is set) an extended description (which
            |you're reading right now). Otherwise, this command simply lists available commands.
            |&{Examples:}
            |Here are some examples of using this command:\n
            | - `..help`: lists all commands\n
            | - `..help osu`: shows general information about the `osu` command\n
            | - `..help rplace -v`: shows very detailed information about the `rplace` command\n
            |Basically, add `-v` (things prefixed with a `-` are called flags) to the end for more
            |detailed help text.
            |&{Reading command usages:}
            |The syntax of the expected command usage is as follows:\n
            | - `name`: denotes that `name` is required, which may be literal or variable\n
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
                                    .sorted()
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
                            |**Usage**: $usage
                        """.trimMargin()

                        if (flags != "-v") {
                            footer { text = "Type '..help ${command.name} -v' for more info." }
                        } else {
                            // The "Extended description" group is always first.
                            val extDescription = " &{Extended description:}" +
                                    command.extDescription.substringAfter("\n")
                            val fieldContents = extDescription
                                .split(singleField)
                                .drop(1)
                                .iterator()

                            // Turn "&{name}" into a field with name [name] and the content of the
                            // part below the tag (until the next field tag).
                            val matcher = singleField.toPattern().matcher(extDescription)
                            val descriptionFields = mutableMapOf<String, String>()
                            while (matcher.find()) {
                                descriptionFields[matcher.group(1)] = fieldContents.next()
                            }

                            for ((fName, fContent) in descriptionFields) {
                                field {
                                    name = fName
                                    content = fContent
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}
