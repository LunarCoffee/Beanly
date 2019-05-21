package dev.lunarcoffee.beanly.exts.commands

import dev.lunarcoffee.beanly.constToEng
import dev.lunarcoffee.beanly.consts.Emoji
import dev.lunarcoffee.beanly.consts.TIME_FORMATTER
import dev.lunarcoffee.beanly.toYesNo
import dev.lunarcoffee.beanly.trimToDescription
import dev.lunarcoffee.framework.api.dsl.command
import dev.lunarcoffee.framework.api.dsl.embed
import dev.lunarcoffee.framework.api.dsl.embedPaginator
import dev.lunarcoffee.framework.api.extensions.error
import dev.lunarcoffee.framework.api.extensions.send
import dev.lunarcoffee.framework.core.annotations.CommandGroup
import dev.lunarcoffee.framework.core.transformers.TrUser
import dev.lunarcoffee.framework.core.transformers.TrWord
import dev.lunarcoffee.framework.core.transformers.utility.Found
import dev.lunarcoffee.framework.core.transformers.utility.SplitTime
import dev.lunarcoffee.framework.core.transformers.utility.UserSearchResult
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.VoiceChannel

@CommandGroup("Info")
class InfoCommands {
    fun ui() = command("ui") {
        description = "Gets info about a user."
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
                            |**Creation time**: ${timeCreated.format(TIME_FORMATTER)}
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
        description = "Gets info about a member of the current server."
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
                            |**Creation time**: ${timeCreated.format(TIME_FORMATTER)}
                            |**Join time**: ${timeJoined.format(TIME_FORMATTER)}
                            |**Avatar ID**: ${user.avatarId ?: "(none)"}
                            |**Mention**: $asMention
                            |**Roles**: ${userRoles.ifEmpty { "(none)" }}
                        """.trimMargin()

                        thumbnail { url = user.avatarUrl ?: user.defaultAvatarUrl }
                    }
                }
            )
        }
    }

    fun ci() = command("ci") {
        description = "Gets info about a channel."
        aliases = listOf("channelinfo")

        extDescription = """
            |`$name [name|id]`\n
            |Gets detailed information about a text or voice channel. If a name or ID is specified,
            |this command will attempt to get a channel with them. Note that even if the name or ID
            |you give me may be valid, I have to be in the server with that channel in order to get
            |info about it. If you don't give me a name or ID, the current channel will be used.
        """.trimToDescription()

        expectedArgs = listOf(TrWord(true))
        execute { ctx, args ->
            val nameOrId = args.get<String>(0)
                .ifEmpty { ctx.event.channel.id }
                .replace("""[#<>]""".toRegex(), "")  // Trim channel mention prefix and suffix.

            val channel = if (nameOrId.toLongOrNull() != null) {
                ctx.jda.getTextChannelById(nameOrId) ?: ctx.jda.getVoiceChannelById(nameOrId)
            } else {
                ctx.jda.getTextChannelsByName(nameOrId, true).firstOrNull()
                    ?: ctx.jda.getVoiceChannelByName(nameOrId, false).firstOrNull()
            }

            if (channel == null) {
                ctx.error("I can't find a text or voice channel with that name or ID!")
                return@execute
            }

            ctx.send(
                embed {
                    channel.run {
                        when (this) {
                            is TextChannel -> {
                                val slowmode = if (slowmode != 0) {
                                    SplitTime(slowmode.toLong()).toString()
                                } else {
                                    "(none)"
                                }

                                title = "${Emoji.MAG_GLASS}  Info on text channel **#$name**:"
                                description = """
                                    |**Channel ID**: $id
                                    |**Server**: ${guild.name}
                                    |**Topic**: ${topic ?: "(none)"}
                                    |**Slowmode**: $slowmode
                                    |**NSFW**: ${isNSFW.toYesNo()}
                                    |**Mention**: $asMention
                                    |**Category**: ${parent?.name ?: "(none)"}
                                    |**Creation time**: ${timeCreated.format(TIME_FORMATTER)}
                                """.trimMargin()
                            }
                            is VoiceChannel -> {
                                val limit = if (userLimit == 0) "(none)" else userLimit.toString()

                                title = "${Emoji.MAG_GLASS}  Info on voice channel **#$name**:"
                                description = """
                                    |**Channel ID**: $id
                                    |**Server**: ${guild.name}
                                    |**Bitrate**: ${bitrate / 1_000}kb/s
                                    |**User limit**: $limit users
                                    |**Mention**: <#$id>
                                    |**Category**: ${parent?.name ?: "(none)"}
                                    |**Creation time**: ${timeCreated.format(TIME_FORMATTER)}
                                """.trimMargin()
                            }
                        }
                    }
                }
            )
        }
    }

    fun ei() = command("ei") {
        description = "Gets info about a custom emote."
        aliases = listOf("emoteinfo")

        extDescription = """
            |`$name name|id`\n
            |Gets detailed information about a custom emote. You must specify the name or ID of the
            |emote you are looking for, and I will attempt to get it for you. Note that even if an
            |ID you provide is valid, I have to be in the server where it comes from in order to be
            |able to get it.
        """.trimToDescription()

        expectedArgs = listOf(TrWord())
        execute { ctx, args ->
            // Trim emote mention characters.
            val nameOrId = args.get<String>(0).replace("""[:<>]""".toRegex(), "")
            val pureId = nameOrId.takeLast(18)

            // Prioritize emotes from the current guild.
            val emote = ctx.guild.getEmotesByName(nameOrId, true).firstOrNull()
                ?: ctx.jda.getEmotesByName(nameOrId, true).firstOrNull()
                ?: if (pureId.toLongOrNull() != null) ctx.jda.getEmoteById(pureId) else null

            if (emote == null) {
                ctx.error("I can't find an emote with that name or ID!")
                return@execute
            }

            ctx.send(
                embed {
                    emote.run {
                        val animated = if (isAnimated) " animated" else ""

                        title = "${Emoji.MAG_GLASS}  Info on$animated emote **$name**:"
                        description = """
                            |**Emote ID**: $id
                            |**Server** ${guild?.name ?: "(none)"}
                            |**Managed**: ${isManaged.toYesNo()}
                            |**Creation time**: ${timeCreated.format(TIME_FORMATTER)}
                            |**Required roles**: ${roles.ifEmpty { "(none)" }}
                        """.trimMargin()

                        thumbnail { url = imageUrl }
                    }
                }
            )
        }
    }

    fun ri() = command("ri") {
        description = "Gets info about a role."
        aliases = listOf("roleinfo")

        extDescription = """
            |`$name [name|id]`\n
            |Gets detailed information about a role. If a name or ID is specified, I will try to
            |get a role with them. Note that even if the ID you provide is valid, I still need to
            |be in the server the role is from in order to get information from it. If a namr or ID
            |is not specified, I will get information about the default role (shows up as @everyone
            |in the server settings).
        """.trimToDescription()

        expectedArgs = listOf(TrWord(true))
        execute { ctx, args ->
            val nameOrId = args.get<String>(0).ifEmpty { ctx.guild.publicRole.id }

            // Prioritize roles from the current guild.
            val role = ctx.guild.getRolesByName(nameOrId, true).firstOrNull()
                ?: ctx.jda.getRolesByName(nameOrId, true).firstOrNull()
                ?: if (nameOrId.toLongOrNull() != null) ctx.jda.getRoleById(nameOrId) else null

            if (role == null) {
                ctx.error("I can't find a role with that name or ID!")
                return@execute
            }

            ctx.send(
                embed {
                    role.run {
                        val roleName = if (role.isPublicRole) {
                            "the public role"
                        } else {
                            "role **@$name**"
                        }
                        val mention = if (guild.id == ctx.guild.id) asMention else "(unavailable)"
                        val permissions = permissions.map { it.constToEng() }.ifEmpty { "(none)" }

                        title = "${Emoji.MAG_GLASS}  Info on $roleName:"
                        description = """
                            |**Role ID**: $id
                            |**Server**: ${guild.name}
                            |**Displayed separately**: ${isHoisted.toYesNo()}
                            |**Normally mentionable**: ${isMentionable.toYesNo()}
                            |**Mention**: $mention
                            |**Creation time**: ${timeCreated.format(TIME_FORMATTER)}
                            |**Managed**: ${isManaged.toYesNo()}
                            |**Permissions**: $permissions
                        """.trimMargin()
                    }
                }
            )
        }
    }

    fun si() = command("si") {
        description = "Gets info about a server."
        aliases = listOf("gi", "guildinfo", "serverinfo")

        extDescription = """
            |`$name [name|id]`\n
            |Gets detailed information about a server. If a name or ID is specified, this command
            |will attempt to get a server with them. Note that even if the name or ID you give is
            |valid, I have to be in the server to be able to find it. If you don't give me a name
            |or ID, the current server will be used instead.
        """.trimToDescription()

        expectedArgs = listOf(TrWord(true))
        execute { ctx, args ->
            val nameOrId = args.get<String>(0).ifEmpty { ctx.guild.id }
            val guild = if (nameOrId.toLongOrNull() != null) {
                ctx.jda.getGuildById(nameOrId)
            } else {
                ctx.jda.getGuildsByName(nameOrId, true).firstOrNull()
            }

            if (guild == null) {
                ctx.error("I can't find a server with that name or ID!")
                return@execute
            }

            ctx.send(
                embedPaginator(ctx.event.author) {
                    guild.run {
                        page(
                            embed {
                                val afkChannel = afkChannel?.id?.run { "<#$this>" } ?: "(none)"
                                val features = features.map { it.constToEng() }

                                title = "${Emoji.MAG_GLASS}  Info on server **$name**:"
                                description = """
                                    |**Guild ID**: $id
                                    |**Total members**: ${members.size} members
                                    |**Total emotes**: ${emotes.size} emotes
                                    |**Total channels**: ${channels.size} channels
                                    |**Text channels**: ${textChannels.size} text channels
                                    |**Voice channels**: ${voiceChannels.size} voice channels
                                    |**AFK channel**: $afkChannel
                                    |**NSFW filter:** ${explicitContentLevel.description}
                                    |**Special features**: ${features.ifEmpty { "(none)" }}
                                """.trimMargin()

                                thumbnail { url = iconUrl }
                            }
                        )
                        page(
                            embed {
                                val roles = if (guild.id == ctx.guild.id) {
                                    roles.map { it.asMention }.toString()
                                } else {
                                    "(unavailable)"
                                }

                                title = "${Emoji.MAG_GLASS}  Info on server **$name**:"
                                description = """
                                    |**Owner**: ${owner?.user?.asTag ?: "(none)"}
                                    |**Voice region**: ${region.getName()}
                                    |**Roles**: ${roles.ifEmpty { "(none)" }}
                                    |**Verification level**: ${verificationLevel.constToEng()}
                                    |**MFA level**: ${requiredMFALevel.constToEng()}
                                    |**Icon ID**: ${iconId ?: "(no icon)"}
                                """.trimMargin()

                                thumbnail { url = iconUrl }
                            }
                        )
                    }
                }
            )
        }
    }
}
