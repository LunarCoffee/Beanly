package framework.dsl

import beanly.consts.EMBED_COLOR
import net.dv8tion.jda.api.entities.EmbedType
import net.dv8tion.jda.api.entities.MessageEmbed

class EmbedDsl {
    var title: String = ""
    var description: String = ""
    var color: Int = EMBED_COLOR

    private val embedFields = mutableListOf<MessageEmbed.Field>()

    fun field(init: FieldDsl.() -> Unit) {
        val fieldDsl = FieldDsl().apply(init)
        embedFields += MessageEmbed.Field(fieldDsl.name, fieldDsl.content, false)
    }

    fun inlineField(init: FieldDsl.() -> Unit) {
        val fieldDsl = FieldDsl().apply(init)
        embedFields += MessageEmbed.Field(fieldDsl.name, fieldDsl.content, true)
    }

    fun create(): MessageEmbed {
        // Use constructor instead of builder to bypass bounds checking to allow for simpler
        // exception handling in higher level APIs.
        return MessageEmbed(
            null,
            title,
            description,
            EmbedType.RICH,
            null,
            color,
            null,
            null,
            null,
            null,
            null,
            null,
            embedFields
        )
    }

    inner class FieldDsl {
        var name: String = ""
        var content: String = ""
    }
}

fun embed(init: EmbedDsl.() -> Unit) = EmbedDsl().apply(init).create()
