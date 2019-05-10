package beanly.exts.commands

import beanly.consts.COL_NAMES
import beanly.consts.DB
import beanly.consts.Emoji
import beanly.exts.commands.utility.banAction
import beanly.exts.commands.utility.muteAction
import beanly.exts.commands.utility.muteInfo
import beanly.exts.commands.utility.timers.MuteTimer
import beanly.trimToDescription
import framework.api.dsl.command
import framework.api.dsl.embed
import framework.api.dsl.embedPaginator
import framework.api.extensions.await
import framework.api.extensions.error
import framework.api.extensions.send
import framework.api.extensions.success
import framework.core.annotations.CommandGroup
import framework.core.transformers.TrInt
import framework.core.transformers.TrRest
import framework.core.transformers.TrTime
import framework.core.transformers.TrUser
import framework.core.transformers.utility.Found
import framework.core.transformers.utility.NotFound
import framework.core.transformers.utility.SplitTime
import framework.core.transformers.utility.UserSearchResult
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.exceptions.PermissionException
import org.litote.kmongo.eq
import java.time.Instant
import java.util.*

@CommandGroup("Moderation")
class ModerationCommands {
    fun mute() = command("mute") {
        val muteCol = DB.getCollection<MuteTimer>(COL_NAMES[MuteTimer::class.simpleName]!!)

        description = "Mutes a member for a specified amount of time."
        aliases = listOf("silence", "softban")

        extDescription = """
            |`$name user time [reason]`\n
            |Mutes a user for a specified amount of time. I must have the permission to manage
            |roles. When a member is muted, they will be sent a message with `time` in a readable
            |format, the provided `reason` (or `(no reason)`) if none is provided, and the user
            |that muted them. You must be able to manage roles to use this command.
        """.trimToDescription()

        expectedArgs = listOf(TrUser(), TrTime(), TrRest(true, "(no reason)"))
        execute { ctx, args ->
            val time = args.get<SplitTime>(1)
            val reason = args.get<String>(2)

            muteAction(ctx, args) { guild, user, offender, mutedRole ->
                val oldRoles = offender.roles
                val pmChannel = user.openPrivateChannel().await()

                try {
                    guild
                        .controller
                        .modifyMemberRoles(offender, listOf(mutedRole), oldRoles)
                        .queue()
                } catch (e: IllegalArgumentException) {
                    ctx.error("I can't remove managed roles!")
                    return@muteAction
                } catch (e: PermissionException) {
                    ctx.error("I don't have enough permissions to do that!")
                    return@muteAction
                }

                ctx.success("`${offender.user.asTag}` has been muted for `$time`!")
                pmChannel.send(
                    embed {
                        title = "${Emoji.HAMMER_AND_WRENCH}  You were muted!"
                        description = """
                            |**Server name**: ${guild.name}
                            |**Muter**: ${ctx.event.author.asTag}
                            |**Time**: $time
                            |**Reason**: $reason
                        """.trimMargin()
                    }
                )

                val muteTimer = MuteTimer(
                    Date.from(Instant.now().plusMillis(time.totalMs)),
                    ctx.guild.id,
                    ctx.event.channel.id,
                    offender.id,
                    oldRoles.map { it.id },
                    mutedRole.id,
                    reason
                )

                // Save in DB for reload on bot relaunch.
                muteCol.insertOne(muteTimer)
                muteTimer.schedule(ctx.event, muteCol)
            }
        }
    }

    fun unmute() = command("unmute") {
        val muteCol = DB.getCollection<MuteTimer>(COL_NAMES[MuteTimer::class.simpleName]!!)

        description = "Unmutes a currently muted member."
        aliases = listOf("unsilence", "unsoftban")

        extDescription = """
            |`$name user`\n
            |Unmutes a muted user. This only works if the user was muted with the `..mute` command
            |from this bot. The unmuted user will be sent a message with the person who unmuted
            |them. You must be able to manage roles to use this command.
        """.trimToDescription()

        expectedArgs = listOf(TrUser())
        execute { ctx, args ->
            muteAction(ctx, args) { guild, user, offender, mutedRole ->
                val timer = muteCol.findOne(MuteTimer::userId eq user.id)
                if (timer == null) {
                    ctx.error("That member isn't muted!")
                    return@muteAction
                }

                val originalRoles = ctx.guild.roles.filter { it.id in timer.prevRoles }
                val pmChannel = user.openPrivateChannel().await()

                try {
                    guild
                        .controller
                        .modifyMemberRoles(offender, originalRoles, listOf(mutedRole))
                        .queue()
                } catch (e: PermissionException) {
                    ctx.error("I don't have enough permissions to do that!")
                    return@muteAction
                }

                ctx.success("`${offender.user.asTag}` has been unmuted!")
                pmChannel.send(
                    embed {
                        title = "${Emoji.HAMMER_AND_WRENCH}  You were manually unmuted!"
                        description = """
                            |**Server name**: ${guild.name}
                            |**Unmuter**: ${ctx.event.author.asTag}
                        """.trimMargin()
                    }
                )
                muteCol.deleteOne(MuteTimer::userId eq offender.id)
            }
        }
    }

    fun mutelist() = command("mutelist") {
        val muteCol = DB.getCollection<MuteTimer>(COL_NAMES[MuteTimer::class.simpleName]!!)

        description = "Shows the muted members on the current server."
        aliases = listOf("silencelist", "softbanlist")

        extDescription = """
            |`$name [user]`\n
            |Without arguments, this command lists all muted members of the current server, along
            |with the remaining time they will be muted for (without a manual unmute). When `user`
            |is provided, this command lists details about their mute, including the reason, the
            |remaining time, and their previous roles.
        """.trimToDescription()

        expectedArgs = listOf(TrUser(true))
        execute { ctx, args ->
            when (val result = args.get<UserSearchResult?>(0)) {
                is Found -> return@execute muteInfo(ctx, muteCol, result.user)
                NotFound -> {
                    ctx.error("I can't find that user!")
                    return@execute
                }
            }

            val mutedPages = muteCol
                .find(MuteTimer::guildId eq ctx.guild.id)
                .toList()
                .associate { it to ctx.guild.getMemberById(it.userId)!! }
                .map { (timer, member) ->
                    val time = SplitTime(timer.time.time - Date().time)
                    "**${member.user.asTag}**: $time"
                }
                .chunked(16)
                .map { it.joinToString("\n") }

            if (mutedPages.isEmpty()) {
                ctx.success("No one in this server is muted!")
                return@execute
            }

            ctx.send(
                embedPaginator(ctx.event.author) {
                    for (members in mutedPages) {
                        page(
                            embed {
                                title = "${Emoji.MUTE}  Currently muted members:"
                                description = members
                            }
                        )
                    }
                }
            )
        }
    }

    fun kick() = command("kick") {
        description = "Kicks a member from a server."
        extDescription = """
            |`$name user [reason]`\n
            |Kicks a user from the current server. I must be have the permission to kick members.
            |When a member is kicked, they will be sent a message with `reason` (or `(no reason)`)
            |if no reason is specified and the user that kicked them. You must be able to kick
            |members to use this command.
        """.trimToDescription()

        expectedArgs = listOf(TrUser(), TrRest(true, "(no reason)"))
        execute { ctx, args ->
            val user = when (val result = args.get<UserSearchResult>(0)) {
                is Found -> result.user
                else -> {
                    ctx.error("I can't find that user!")
                    return@execute
                }
            }

            val reason = args.get<String>(1)
            val guild = ctx.guild

            // Make sure the author can kick members.
            val guildAuthor = guild.getMember(ctx.event.author) ?: return@execute
            if (!guildAuthor.hasPermission(Permission.KICK_MEMBERS)) {
                ctx.error("You need to be able to kick members!")
                return@execute
            }

            val offender = guild.getMember(user)
            if (offender == null) {
                ctx.error("That user is not a member of this server!")
                return@execute
            }

            if (!guild.selfMember.canInteract(offender)) {
                ctx.error("I don't have enough permissions to do that!")
                return@execute
            }

            try {
                guild.controller.kick(offender, reason).await()
                ctx.success("`${offender.user.asTag}` has been kicked!")

                // Send PM to kicked user with information.
                user.openPrivateChannel().await().send(
                    embed {
                        title = "${Emoji.HAMMER_AND_WRENCH}  You were kicked!"
                        description = """
                            |**Server name**: ${guild.name}
                            |**Kicker**: ${ctx.event.author.asTag}
                            |**Reason**: $reason
                        """.trimMargin()
                    }
                )
            } catch (e: PermissionException) {
                ctx.error("I don't have enough permissions to do that!")
            }
        }
    }

    fun ban() = command("ban") {
        description = "Permanently bans a member from a server."
        extDescription = """
            |`$name user [reason]`\n
            |Bans a user from the current server. I must be have the permission to ban members.
            |When a member is banned, they will be sent a message with `reason` (or `(no reason)`)
            |if no reason is specified and the user that banned them. You must be able to ban
            |members to use this command.
        """.trimToDescription()

        expectedArgs = listOf(TrUser(), TrRest(true, "(no reason)"))
        execute { ctx, args ->
            val reason = args.get<String>(1)

            banAction(ctx, args) { guild, user ->
                val offender = guild.getMember(user)
                if (offender == null) {
                    ctx.error("That user is not a member of this server!")
                    return@banAction
                }

                if (!guild.selfMember.canInteract(offender)) {
                    ctx.error("I don't have enough permissions to do that!")
                    return@banAction
                }

                guild.controller.ban(offender, 0, reason).await()
                ctx.success("`${offender.user.asTag}` has been banned!")

                // Send PM to banned user with information.
                offender.user.openPrivateChannel().await().send(
                    embed {
                        title = "${Emoji.HAMMER_AND_WRENCH}  You were banned!"
                        description = """
                            |**Server name**: ${guild.name}
                            |**Banner**: ${ctx.event.author.asTag}
                            |**Reason**: $reason
                        """.trimMargin()
                    }
                )
            }
        }
    }

    fun unban() = command("unban") {
        description = "Unbans a member from a server."
        extDescription = """
            |`$name user`\n
            |Unbans a banned user from the current server. I must have the permission to ban
            |members. When a member is unbanned, they will be sent a message with the person who
            |unbanned them. You must be able to ban members to use this command.
        """.trimToDescription()

        expectedArgs = listOf(TrUser())
        execute { ctx, args ->
            banAction(ctx, args) { guild, user ->
                guild.controller.unban(user).await()
                ctx.success("`${user.asTag}` has been unbanned!")

                // Send PM to unbanned user with information.
                user.openPrivateChannel().await().send(
                    embed {
                        title = "${Emoji.HAMMER_AND_WRENCH}  You were unbanned!"
                        description = """
                            |**Server name**: ${guild.name}
                            |**Unbanner**: ${ctx.event.author.asTag}
                        """.trimMargin()
                    }
                )
            }
        }
    }

    fun purge() = command("purge") {
        description = "Deletes a certain amount of messages from a channel."
        aliases = listOf("clear", "massdelete")

        extDescription = """
            |`$name limit [user]`\n
            |Deletes the past `limit` messages from the current channel, the message containing the
            |command exempt. If `user` is specified, this command deletes the past `limit` messages
            |from only that user. You must be able to manage messages to use this command.
        """.trimToDescription()

        expectedArgs = listOf(TrInt(), TrUser(true))
        execute { ctx, args ->
            val limit = args.get<Int>(0)
            val user = when (val result = args.get<UserSearchResult?>(1)) {
                is Found -> result.user
                null -> null
                else -> {
                    ctx.error("I can't find that user!")
                    return@execute
                }
            }

            // Make sure the author can manage messages.
            val guildAuthor = ctx.guild.getMember(ctx.event.author) ?: return@execute
            if (!guildAuthor.hasPermission(Permission.MESSAGE_MANAGE)) {
                ctx.error("You need to be able to manage messages!")
                return@execute
            }

            // Don't get rate limited!
            if (limit !in 1..100) {
                ctx.error("I can't purge that amount of messages!")
                return@execute
            }

            val channel = ctx.event.channel
            try {
                channel.purgeMessages(
                    if (user != null) {
                        channel
                            .iterableHistory
                            .asSequence()
                            .filter { it.author == user }
                            .take(limit + if (user == ctx.event.author) 1 else 0)
                            .toList()
                    } else {
                        channel.iterableHistory.take(limit + 1)
                    }
                )
            } catch (e: IllegalArgumentException) {
                ctx.error("I can't delete some messages because they're too old!")
            } catch (e: PermissionException) {
                ctx.error("I don't have enough permissions to do that!")
            }
        }
    }

    fun slowmode() = command("slow") {
        description = "Sets the current channel's slowmode."
        aliases = listOf("cooldown", "slowmode")

        extDescription = """
            |`$name time`
            |When setting the slowmode cooldown of a channel in the Discord client's channel
            |settings, the only options available are at fixed lengths of time. This command lets
            |you change it to any arbitrary time between none to six hours. The `time` argument
            |should look something like `2m 30s`, `1h`, or `0s`, to give some examples.
        """.trimToDescription()

        expectedArgs = listOf(TrTime())
        execute { ctx, args ->
            val slowmode = args.get<SplitTime>(0)
            val slowmodeSeconds = slowmode.totalMs.toInt() / 1_000

            if (slowmodeSeconds !in 0..21_600) {
                ctx.error("I can't set this channel's slowmode to that amount of time!")
                return@execute
            }

            val channel = ctx.guild.getTextChannelById(ctx.event.channel.id) ?: return@execute
            channel.manager.setSlowmode(slowmodeSeconds).queue()

            val slowmodeRepr = if (slowmode.totalMs > 0) "`$slowmode`" else "disabled"
            ctx.success("This channel's slowmode time is now `$slowmodeRepr`!")
        }
    }
}
