package dev.lunarcoffee.beanly.exts.commands.utility.tags

import dev.lunarcoffee.beanly.consts.DB
import dev.lunarcoffee.beanly.consts.Emoji
import dev.lunarcoffee.framework.api.dsl.embed
import dev.lunarcoffee.framework.api.dsl.embedPaginator
import dev.lunarcoffee.framework.api.extensions.error
import dev.lunarcoffee.framework.api.extensions.send
import dev.lunarcoffee.framework.api.extensions.success
import dev.lunarcoffee.framework.core.CommandContext
import dev.lunarcoffee.framework.core.transformers.utility.SplitTime
import org.bson.conversions.Bson
import org.litote.kmongo.and
import org.litote.kmongo.eq
import java.util.*

class TagManager {
    suspend fun sendTags(ctx: CommandContext) {
        // Sort tag entries by the time they were created at.
        val tags = tagCol
            .find(Tag::guildId eq ctx.guild.id)
            .toList()
            .sortedByDescending { it.timeCreated }
            .chunked(16)

        if (tags.isEmpty()) {
            ctx.success("There are no tags in this server!")
            return
        }

        ctx.send(
            embedPaginator(ctx.event.author) {
                for (tagPage in tags) {
                    page(
                        embed {
                            title = "${Emoji.BOOKMARK}  Tags in this server:"
                            description = tagPage.joinToString("\n") {
                                val timeMs = it.timeCreated.toInstant().toEpochMilli()
                                val time = SplitTime(timeMs - System.currentTimeMillis())
                                    .localWithoutWeekday()

                                "**${it.name}**: $time"
                            }
                        }
                    )
                }
            }
        )
    }

    suspend fun sendOneTag(ctx: CommandContext, name: String) {
        val tag = tagCol.findOne(sameTag(ctx, name))
        if (tag == null) {
            ctx.error("There is no tag with that name!")
            return
        }

        ctx.send(
            embed {
                tag.run {
                    val authorTag = ctx.jda.getUserById(authorId)?.asTag ?: "(none)"
                    val timeMs = timeCreated.toInstant().toEpochMilli()
                    val time = SplitTime(timeMs - System.currentTimeMillis())
                        .localWithoutWeekday()

                    title = "${Emoji.BOOKMARK}  Tag **${this@run.name}**:"
                    description = """
                        |**Author**: $authorTag
                        |**Time created**: $time
                    """.trimMargin()

                    field {
                        this@field.name = "Content:"
                        content = this@run.content
                    }
                }
            }
        )
    }

    suspend fun addTag(ctx: CommandContext, name: String, content: String) {
        if (name.length > 30 || content.length > 1_000) {
            ctx.error("The name or content of your tag is too long!")
            return
        }

        // Don't allow duplicates in the same guild.
        if (tagCol.findOne(sameTag(ctx, name)) != null) {
            ctx.error("A tag with that name already exists!")
            return
        }

        tagCol.insertOne(Tag(ctx.guild.id, ctx.event.author.id, name, content, Date()))
        ctx.success("Your tag has been created!")
    }

    suspend fun editTag(ctx: CommandContext, name: String, content: String) {
        val tag = tagCol.findOne(sameTag(ctx, name))
        if (tag == null) {
            ctx.error("There is no tag with that name!")
            return
        }

        // Only let people edit their own tags.
        if (tag.authorId != ctx.event.author.id) {
            ctx.error("You can only edit your tags!")
            return
        }

        tagCol.updateOne(sameTag(ctx, name), tag.apply { this@apply.content = content })
        ctx.success("Your tag has been edited!")
    }

    suspend fun deleteTag(ctx: CommandContext, name: String) {
        val tag = tagCol.findOne(sameTag(ctx, name))
        if (tag == null) {
            ctx.error("There is no tag with that name!")
            return
        }

        // Only let people delete their own tags.
        if (tag.authorId != ctx.event.author.id) {
            ctx.error("You can only edit your tags!")
            return
        }

        tagCol.deleteOne(sameTag(ctx, name))
        ctx.success("Your tag has been deleted!")
    }

    // Matches tags by guild ID and name.
    private fun sameTag(ctx: CommandContext, name: String): Bson {
        return and(Tag::guildId eq ctx.guild.id, Tag::name eq name)
    }

    companion object {
        val tagCol = DB.getCollection<Tag>("Tags1")
    }
}
