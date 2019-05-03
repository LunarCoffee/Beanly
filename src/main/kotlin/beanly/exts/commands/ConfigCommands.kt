package beanly.exts.commands

import beanly.consts.DB
import beanly.consts.NO_PAY_RESPECTS_COL_NAME
import beanly.exts.commands.utility.NoPayRespects
import beanly.trimToDescription
import framework.api.dsl.command
import framework.api.extensions.success
import framework.core.annotations.CommandGroup
import org.litote.kmongo.eq

@CommandGroup("Config")
class ConfigCommands {
    fun togglef() = command("togglef") {
        val noPayRespectsCol = DB.getCollection<NoPayRespects>(NO_PAY_RESPECTS_COL_NAME)

        description = "Toggles the fancy F to pay respects embed."
        aliases = listOf("togglepayrespects")

        extDescription = """
            |`togglef`\n
            |When you type `f` or `F`, by default, I will replace your message with a fancy embed
            |that allows other people to react to it with a regional indicator F emoji. When such a
            |reaction is added, it adds their name to the list on the embed. This command toggles
            |that behavior.
        """.trimToDescription()

        execute { ctx, _ ->
            val guildId = ctx.guild.id
            noPayRespectsCol.run {
                if (findOne(NoPayRespects::guildId eq guildId) == null) {
                    insertOne(NoPayRespects(guildId))
                    ctx.success("Disabled the pay respects embed!")
                } else {
                    deleteOne(NoPayRespects::guildId eq guildId)
                    ctx.success("Enabled the pay respects embed!")
                }
            }
        }
    }
}
