package beanly.exts.commands

import beanly.consts.GUILD_OVERRIDES
import beanly.exts.commands.utility.GO
import beanly.exts.commands.utility.GuildOverrides
import beanly.trimToDescription
import framework.api.dsl.command
import framework.api.extensions.success
import framework.core.annotations.CommandGroup
import org.litote.kmongo.eq
import org.litote.kmongo.set

@CommandGroup("Config")
class ConfigCommands {
    fun togglecs() = command("togglecs") {
        description = "Toggles suggestions for when you type something wrong."
        aliases = listOf("togglesuggestions", "togglecommandsuggestions")

        extDescription = """
            |`$name`\n
            |When you type a command and spell the name wrong, I might try and guess what you were
            |trying to do. This comes in the form of command suggestions, which suggest a potential
            |command that has a name that is close to what you typed. They delete themselves after
            |five seconds as to not clog up the channel. This command toggles the sending of these
            |suggestions.
        """.trimToDescription()

        execute { ctx, _ ->
            val guildId = ctx.guild.id

            GUILD_OVERRIDES.run {
                val override = findOne(GO::id eq guildId)

                when {
                    override == null -> {
                        insertOne(GuildOverrides(guildId, false, true))
                        ctx.success("Disabled command suggestions!")
                    }
                    override.noSuggestCommands -> {
                        updateOne(GO::id eq guildId, set(GO::noSuggestCommands, false))
                        ctx.success("Enabled command suggestions!")
                    }
                    else -> {
                        updateOne(GO::id eq guildId, set(GO::noSuggestCommands, true))
                        ctx.success("Disabled command suggestions!")
                    }
                }
            }
        }
    }

    fun togglef() = command("togglef") {
        description = "Toggles the fancy F to pay respects embed."
        aliases = listOf("togglepayrespects")

        extDescription = """
            |`$name`\n
            |When you type `f` or `F`, by default, I will replace your message with a fancy embed
            |that allows other people to react to it with a regional indicator F emoji. When such a
            |reaction is added, it adds their name to the list on the embed. This command toggles
            |that behavior.
        """.trimToDescription()

        execute { ctx, _ ->
            val guildId = ctx.guild.id

            GUILD_OVERRIDES.run {
                val override = findOne(GO::id eq guildId)
                when {
                    override == null -> {
                        insertOne(GuildOverrides(guildId, true, true))
                        ctx.success("Disabled the pay respects embed!")
                    }
                    override.noPayRespects -> {
                        updateOne(GO::id eq guildId, set(GO::noPayRespects, false))
                        ctx.success("Enabled the pay respects embed!")
                    }
                    else -> {
                        updateOne(GO::id eq guildId, set(GO::noPayRespects, true))
                        ctx.success("Disabled the pay respects embed!")
                    }
                }
            }
        }
    }
}
