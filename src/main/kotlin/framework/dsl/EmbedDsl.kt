package framework.dsl

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.Role

class EmbedDsl : EmbedBuilder() {
    var title: String = ""
    var description: String = ""
    var color: Int = Role.DEFAULT_COLOR_RAW

    fun field(init: FieldDsl.() -> Unit) {
        val fieldDsl = FieldDsl().apply(init)
        addField(MessageEmbed.Field(fieldDsl.name, fieldDsl.content, false))
    }

    fun inlineField(init: FieldDsl.() -> Unit) {
        val fieldDsl = FieldDsl().apply(init)
        addField(MessageEmbed.Field(fieldDsl.name, fieldDsl.content, true))
    }

    fun create(): MessageEmbed {
        return apply {
            setTitle(title)
            setDescription(description)
            setColor(color)
        }.build()
    }

    inner class FieldDsl {
        var name: String = ""
        var content: String = ""
    }
}

fun embed(init: EmbedDsl.() -> Unit) = EmbedDsl().apply(init).create()
