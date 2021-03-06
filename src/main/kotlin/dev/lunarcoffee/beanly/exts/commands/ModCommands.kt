package dev.lunarcoffee.beanly.exts.commands

import dev.lunarcoffee.beanly.constToEng
import dev.lunarcoffee.beanly.consts.COL_NAMES
import dev.lunarcoffee.beanly.consts.DB
import dev.lunarcoffee.beanly.consts.Emoji
import dev.lunarcoffee.beanly.consts.TIME_FORMATTER
import dev.lunarcoffee.beanly.exts.commands.utility.banAction
import dev.lunarcoffee.beanly.exts.commands.utility.getAuditTargetName
import dev.lunarcoffee.beanly.exts.commands.utility.muteAction
import dev.lunarcoffee.beanly.exts.commands.utility.muteInfo
import dev.lunarcoffee.beanly.exts.commands.utility.timers.MuteTimer
import dev.lunarcoffee.beanly.trimToDescription
import dev.lunarcoffee.framework.api.dsl.command
import dev.lunarcoffee.framework.api.dsl.embed
import dev.lunarcoffee.framework.api.dsl.embedPaginator
import dev.lunarcoffee.framework.api.extensions.await
import dev.lunarcoffee.framework.api.extensions.error
import dev.lunarcoffee.framework.api.extensions.send
import dev.lunarcoffee.framework.api.extensions.success
import dev.lunarcoffee.framework.core.annotations.CommandGroup
import dev.lunarcoffee.framework.core.transformers.TrInt
import dev.lunarcoffee.framework.core.transformers.TrRest
import dev.lunarcoffee.framework.core.transformers.TrTime
import dev.lunarcoffee.framework.core.transformers.TrUser
import dev.lunarcoffee.framework.core.transformers.utility.Found
import dev.lunarcoffee.framework.core.transformers.utility.NotFound
import dev.lunarcoffee.framework.core.transformers.utility.SplitTime
import dev.lunarcoffee.framework.core.transformers.utility.UserSearchResult
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.exceptions.PermissionException
import org.litote.kmongo.eq
import java.time.Instant
import java.util.*

@CommandGroup("Mod")
class ModCommands {
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

    fun logs() = command("logs") {
        description = "Gets audit log history."
        aliases = listOf("audits", "auditlogs")

        extDescription = """
            |`$name [limit]`\n
            |This command retrieves the last `limit` entries in the audit log. If `limit` is not
            |given, I will get the last ten entries. For each audit log entry, I'll show the type
            |of the audit, the user that initiated it, the affected target type and name, the time
            |at which it took place, and the reason (when a user is banned, for example).
            |&{Limitations:}
            |I won't show you what actually changed, since that would require more effort for me to
            |do than for you to open up the audit logs in the server settings. You need to be able
            |to view the logs already to use this command, anyway.
        """.trimToDescription()

        expectedArgs = listOf(TrInt(true, 10))
        execute { ctx, args ->
            val limit = args.get<Int>(0)
            if (limit !in 1..100) {
                ctx.error("I can't get that many log entries!")
                return@execute
            }

            // Make sure the author can normally check audit logs.
            val guildAuthor = ctx.guild.getMember(ctx.event.author) ?: return@execute
            if (!guildAuthor.hasPermission(Permission.VIEW_AUDIT_LOGS)) {
                ctx.error("You need to be able to view audit logs!")
                return@execute
            }

            val logs = try {
                ctx.guild.retrieveAuditLogs().takeAsync(limit).await()
            } catch (e: InsufficientPermissionException) {
                ctx.error("I need to be able to view this server's audit logs!")
                return@execute
            }

            val guildName = ctx.guild.name
            ctx.send(
                embedPaginator(ctx.event.author) {
                    for (log in logs) {
                        page(
                            embed {
                                log.run {
                                    // Name of audit target based on its type.
                                    val name = runBlocking {
                                        getAuditTargetName(ctx, targetType, targetId)
                                    }

                                    title = "${Emoji.OPEN_BOOK}  Audit logs of **$guildName**:"
                                    description = """
                                        |**Audit ID**: $id
                                        |**Type**: ${type.constToEng()}
                                        |**Initiator**: ${user?.asTag ?: "(none)"}
                                        |**Target type**: ${targetType.name.toLowerCase()}
                                        |**Target**: $name
                                        |**Time occurred**: ${timeCreated.format(TIME_FORMATTER)}
                                        |**Reason**: ${reason ?: "(no reason)"}
                                    """.trimMargin()
                                }
                            }
                        )
                    }
                }
            )
        }
    }
}
