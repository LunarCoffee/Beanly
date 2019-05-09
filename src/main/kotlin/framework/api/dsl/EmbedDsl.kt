@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package framework.api.dsl

import beanly.consts.EMBED_COLOR
import net.dv8tion.jda.api.entities.EmbedType
import net.dv8tion.jda.api.entities.MessageEmbed

class EmbedDsl {
    var url: String = ""
    var title: String = ""
    var description: String = ""
    var color: Int = EMBED_COLOR

    private var author: MessageEmbed.AuthorInfo? = null
    private var footer: MessageEmbed.Footer? = null
    private var thumbnail: MessageEmbed.Thumbnail? = null
    private var image: MessageEmbed.ImageInfo? = null

    private val embedFields = mutableListOf<MessageEmbed.Field>()

    fun field(init: FieldDsl.() -> Unit) {
        val fieldDsl = FieldDsl().apply(init)
        embedFields += MessageEmbed.Field(fieldDsl.name, fieldDsl.content, false)
    }

    fun inlineField(init: FieldDsl.() -> Unit) {
        val fieldDsl = FieldDsl().apply(init)
        embedFields += MessageEmbed.Field(fieldDsl.name, fieldDsl.content, true)
    }

    inner class FieldDsl {
        var name: String = ""
        var content: String = ""
    }

    fun footer(init: FooterDsl.() -> Unit) {
        val footerDsl = FooterDsl().apply(init)
        footer = MessageEmbed.Footer(footerDsl.text, footerDsl.iconUrl, footerDsl.proxyIconUrl)
    }

    inner class FooterDsl {
        var text: String? = null
        var iconUrl: String? = null
        var proxyIconUrl: String? = null
    }

    fun author(init: AuthorDsl.() -> Unit) {
        val authorDsl = AuthorDsl().apply(init)
        author = authorDsl.run {
            MessageEmbed.AuthorInfo(name, url, iconUrl, proxyIconUrl)
        }
    }

    inner class AuthorDsl {
        var name: String? = null
        var url: String? = null
        var iconUrl: String? = null
        var proxyIconUrl: String? = null
    }

    fun thumbnail(init: ImageOrThumbnailDsl.() -> Unit) {
        val thumbnailDsl = ImageOrThumbnailDsl().apply(init)
        thumbnail = thumbnailDsl.run {
            MessageEmbed.Thumbnail(url, proxyUrl, width, height)
        }
    }

    fun image(init: ImageOrThumbnailDsl.() -> Unit) {
        val imageDsl = ImageOrThumbnailDsl().apply(init)
        image = imageDsl.run {
            MessageEmbed.ImageInfo(url, proxyUrl, width, height)
        }
    }

    inner class ImageOrThumbnailDsl {
        var url: String? = null
        var proxyUrl: String? = null
        var width: Int = 50
        var height: Int = 50
    }

    fun create(): MessageEmbed {
        // Use constructor instead of builder to bypass bounds checking to allow for simpler
        // exception handling in higher level APIs.
        return MessageEmbed(
            url,
            title,
            description,
            EmbedType.RICH,
            null,
            color,
            thumbnail,
            null,
            author,
            null,
            footer,
            image,
            embedFields
        )
    }
}

inline fun embed(crossinline init: EmbedDsl.() -> Unit) = EmbedDsl().apply(init).create()
