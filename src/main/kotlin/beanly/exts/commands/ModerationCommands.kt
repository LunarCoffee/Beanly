package beanly.exts.commands

import beanly.consts.DB
import beanly.consts.Emoji
import beanly.consts.MUTE_TIMERS_COL_NAME
import beanly.exts.commands.utility.timers.MuteTimer
import beanly.trimToDescription
import framework.api.dsl.command
import framework.api.dsl.embed
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
import framework.core.transformers.utility.SplitTime
import framework.core.transformers.utility.UserSearchResult
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.exceptions.PermissionException
import java.time.Instant
import java.util.*

@CommandGroup("Moderation")
class ModerationCommands {
    fun mute() = command("mute") {
        val muteCol = DB.getCollection<MuteTimer>(MUTE_TIMERS_COL_NAME)

        description = "Mutes a member for a specified amount of time."
        aliases = listOf("silence")

        extDescription = """
            |`$name user time [reason]`\n
            |Mutes a user for a specified amount of time. I must have the permission to manage
            |roles. When a member is kicked, they will be sent a message with `time` in a readable
            |format, the provided `reason` (or `(no reason)`) if none is provided, and the user
            |that muted them. You must be able to manage roles to use this command.
        """.trimToDescription()

        expectedArgs = listOf(TrUser(), TrTime(), TrRest(true, "(no reason)"))
        execute { ctx, args ->
            val user = when (val result = args.get<UserSearchResult>(0)) {
                is Found -> result.user
                else -> {
                    ctx.error("I can't find that user!")
                    return@execute
                }
            }

            val time = args.get<SplitTime>(1)
            val reason = args.get<String>(2)
            val guild = ctx.guild

            // This uses the permission to manage roles as an allowance to mute.
            val guildAuthor = guild.getMember(ctx.event.author) ?: return@execute
            if (!guildAuthor.hasPermission(Permission.MANAGE_ROLES)) {
                ctx.error("You need to be able to manage roles to mute users!")
                return@execute
            }

            val offender = guild.getMember(user)
            if (offender == null) {
                ctx.error("That user is not a member of this server!")
                return@execute
            }

            // Assume the guild has a role with "muted" in it, and get it.
            val mutedRole = guild.roles.find { it.name.contains("muted", true) }
            if (mutedRole == null) {
                ctx.error("I need to be able to assign a role with `muted` in its name!")
                return@execute
            }

            val oldRoles = offender.roles
            val pmChannel = user.openPrivateChannel().await()

            try {
                guild.controller.modifyMemberRoles(offender, listOf(mutedRole), oldRoles).queue()
            } catch (e: PermissionException) {
                ctx.error("I don't have enough permissions to do that!")
                return@execute
            }

            ctx.success("`${offender.user.asTag}` has been muted!")
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
            if (!guildAuthor.hasPermission(Permission.BAN_MEMBERS)) {
                ctx.error("You need to be able to ban members!")
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
                guild.controller.ban(offender, 0, reason).await()
                ctx.success("`${offender.user.asTag}` has been banned!")

                // Send PM to kicked user with information.
                user.openPrivateChannel().await().send(
                    embed {
                        title = "${Emoji.HAMMER_AND_WRENCH}  You were banned!"
                        description = """
                            |**Server name**: ${guild.name}
                            |**Banner**: ${ctx.event.author.asTag}
                            |**Reason**: $reason
                        """.trimMargin()
                    }
                )
            } catch (e: PermissionException) {
                ctx.error("I don't have enough permissions to do that!")
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
}
