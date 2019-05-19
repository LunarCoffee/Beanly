package dev.lunarcoffee.beanly.exts.commands.utility

import dev.lunarcoffee.beanly.consts.Emoji
import dev.lunarcoffee.beanly.exts.commands.utility.timers.MuteTimer
import dev.lunarcoffee.framework.api.dsl.embed
import dev.lunarcoffee.framework.api.extensions.await
import dev.lunarcoffee.framework.api.extensions.error
import dev.lunarcoffee.framework.api.extensions.send
import dev.lunarcoffee.framework.core.CommandArguments
import dev.lunarcoffee.framework.core.CommandContext
import dev.lunarcoffee.framework.core.transformers.utility.Found
import dev.lunarcoffee.framework.core.transformers.utility.SplitTime
import dev.lunarcoffee.framework.core.transformers.utility.UserSearchResult
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.audit.AuditLogChange
import net.dv8tion.jda.api.audit.TargetType
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.exceptions.PermissionException
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq
import java.util.*

suspend inline fun muteAction(
    ctx: CommandContext,
    args: CommandArguments,
    crossinline action: suspend (Guild, User, Member, Role) -> Unit
) {
    val user = when (val result = args.get<UserSearchResult>(0)) {
        is Found -> result.user
        else -> {
            ctx.error("I can't find that user!")
            return
        }
    }
    val guild = ctx.guild

    // This uses the permission to manage roles as an allowance to mute.
    val guildAuthor = guild.getMember(ctx.event.author) ?: return
    if (!guildAuthor.hasPermission(Permission.MANAGE_ROLES)) {
        ctx.error("You need to be able to manage roles to mute users!")
        return
    }

    val offender = guild.getMember(user)
    if (offender == null) {
        ctx.error("That user is not a member of this server!")
        return
    }

    // Assume the guild has a role with "muted" in it, and get it.
    val mutedRole = guild.roles.find { it.name.contains("muted", true) }
    if (mutedRole == null) {
        ctx.error("I need to be able to assign a role with `muted` in its name!")
        return
    }

    // This is a mute or unmute command.
    action(guild, user, offender, mutedRole)
}

// Sends information about a muted user.
suspend fun muteInfo(ctx: CommandContext, muteCol: CoroutineCollection<MuteTimer>, user: User) {
    val member = ctx.event.guild.getMember(user)
    if (member == null) {
        ctx.error("That user is not a member of this server!")
        return
    }

    val timer = muteCol.findOne(MuteTimer::userId eq member.id)
    if (timer == null) {
        ctx.error("That member isn't muted!")
        return
    }

    val time = SplitTime(timer.time.time - Date().time)
    val prevRoles = timer
        .prevRoles
        .mapNotNull { ctx.guild.getRoleById(it)?.asMention }

    ctx.send(
        embed {
            title = "${Emoji.MUTE}  Info on muted member **${member.user.asTag}**:"
            description = """
                |**Time remaining**: $time
                |**Previous roles**: $prevRoles
                |**Reason**: ${timer.reason}
            """.trimMargin()
        }
    )
}

suspend inline fun banAction(
    ctx: CommandContext,
    args: CommandArguments,
    crossinline action: suspend (Guild, User) -> Unit
) {
    val user = when (val result = args.get<UserSearchResult>(0)) {
        is Found -> result.user
        else -> {
            ctx.error("I can't find that user!")
            return
        }
    }
    val guild = ctx.guild

    // Make sure the author can kick members.
    val guildAuthor = guild.getMember(ctx.event.author) ?: return
    if (!guildAuthor.hasPermission(Permission.BAN_MEMBERS)) {
        ctx.error("You need to be able to ban members!")
        return
    }

    try {
        // This is a ban or unban command.
        action(guild, user)
    } catch (e: PermissionException) {
        ctx.error("I don't have enough permissions to do that!")
    }
}

// Generates a name for an audit action. For example, if [type] is [TargetType.MEMBER], this will
// return the tag of the member. If [type] is [TargetType.ROLE], this will return the role's name
// as a mention.
suspend fun getAuditTargetName(ctx: CommandContext, type: TargetType, id: String): String {
    return when (type) {
        TargetType.GUILD -> ctx.jda.getGuildById(id)!!.name
        TargetType.CHANNEL -> {
            val textChannel = ctx.guild.getTextChannelById(id)
            val voiceChannel = ctx.guild.getVoiceChannelById(id)

            val name = textChannel?.asMention ?: voiceChannel?.name?.run { "VC `$this`" }
            name ?: "(unavailable)"
        }
        TargetType.ROLE -> ctx.jda.getRoleById(id)?.asMention ?: "(unavailable)"
        TargetType.MEMBER -> ctx.guild.getMemberById(id)?.user?.asTag ?: "(unavailable)"
        TargetType.INVITE -> "(some invite link)"
        TargetType.WEBHOOK -> ctx.jda.retrieveWebhookById(id).await().name
        TargetType.EMOTE -> ctx.jda.getEmoteById(id)?.asMention ?: "(unavailable)"
        else -> "(unavailable)"
    }
}
