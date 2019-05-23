package dev.lunarcoffee.beanly.exts.commands.oyster

import dev.lunarcoffee.beanly.consts.DB
import dev.lunarcoffee.beanly.consts.DEFAULT_TIMER
import dev.lunarcoffee.framework.api.dsl.embed
import dev.lunarcoffee.framework.api.dsl.embedPaginator
import dev.lunarcoffee.framework.api.extensions.error
import dev.lunarcoffee.framework.api.extensions.send
import dev.lunarcoffee.framework.api.extensions.success
import dev.lunarcoffee.framework.core.CommandContext
import org.litote.kmongo.eq
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.schedule
import kotlin.random.Random

object OysterManager {
    private val userCol = DB.getCollection<OysterUser>("OysterUsers7")

    // User IDs of users currently on cooldown.
    private val onCooldown = ConcurrentHashMap<String, Long>()

    private const val cooldownMs = 5_000L
    private const val part = 4
    private const val chance = 7

    suspend fun sendCatches(ctx: CommandContext) {
        val user = userCol.findOne(isSame(ctx.event.author.id))
        if (user == null || user.catches.isEmpty()) {
            ctx.success("You haven't caught anything!")
            return
        }

        val catches = user
            .catches
            .map { (catchName, amount) -> "**$catchName**: $amount" }
            .sortedBy {
                when (it[2].toString()) {
                    OysterCatchRarity.COMMON.prefix -> 0
                    OysterCatchRarity.UNCOMMON.prefix -> 1
                    OysterCatchRarity.RARE.prefix -> 2
                    else -> 3
                }
            }
            .chunked(16)
            .map { it.joinToString("\n") }

        ctx.send(
            embedPaginator(ctx.event.author) {
                val oysterEmote = ctx.jda.getEmoteById(580263164328017923)!!.asMention
                for (catchPage in catches) {
                    page(
                        embed {
                            title = "$oysterEmote  Your catches:"
                            description = catchPage

                            footer {
                                text = "Type '..help oyster -v' for info on the prefixes."
                            }
                        }
                    )
                }
            }
        )
    }

    suspend fun handleCooldown(ctx: CommandContext): Boolean {
        val userId = ctx.event.author.id
        if (userId in onCooldown.keys) {
            val remaining = (onCooldown[userId]!! - System.currentTimeMillis()) / 1_000
            ctx.error("You need to wait `$remaining` seconds before you do that again!")
            return true
        }
        return false
    }

    suspend fun generateCatch(ctx: CommandContext) {
        val catch = catch(ctx)
        val userId = ctx.event.author.id

        if (catch == OysterCatch.EMPTY) {
            ctx.error("You didn't catch anything!")
        } else {
            val oysterEmote = ctx.jda.getEmoteById(580263164328017923)!!.asMention
            ctx.send(
                embed {
                    catch.run {
                        title = "$oysterEmote  You caught a **$nameWithPrefix**!"
                        this@embed.description = description
                    }
                }
            )
        }

        // Schedule cooldown. No persistence since the cooldown should be short enough that a bot
        // shutdown or restart would take longer.
        onCooldown[userId] = System.currentTimeMillis() + cooldownMs
        DEFAULT_TIMER.schedule(cooldownMs) { onCooldown -= userId }
    }

    private suspend fun catch(ctx: CommandContext): OysterCatch {
        val catch = if (Random.nextInt(chance) < part) {
            OysterCatch.catches.random()
        } else {
            return OysterCatch.EMPTY
        }

        val userId = ctx.event.author.id
        val user = userCol.findOne(isSame(userId))

        if (user == null) {
            userCol.insertOne(OysterUser(userId, mutableMapOf(catch.nameWithPrefix to 1)))
        } else {
            val name = catch.nameWithPrefix
            user.catches.apply {
                putIfAbsent(name, 0)
                this[name] = this[name]!! + 1
            }
            userCol.updateOne(isSame(userId), user)
        }
        return catch
    }

    private fun isSame(userId: String) = OysterUser::userId eq userId
}
