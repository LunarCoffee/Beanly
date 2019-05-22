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

class OysterManager {
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
        DEFAULT_TIMER.schedule(cooldownMs) { onCooldown.remove(userId) }
    }

    private suspend fun catch(ctx: CommandContext): OysterCatch {
        val catch = if (Random.nextInt(chance) < t) catches.random() else return OysterCatch.EMPTY
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

    companion object {
        private val userCol = DB.getCollection<OysterUser>("OysterUsers7")

        // User IDs of users currently on cooldown.
        private val onCooldown = ConcurrentHashMap<String, Long>()

        private const val cooldownMs = 5_000L
        private const val t = 4
        private const val chance = 7

        // Weighting of 24; pretty useless, mostly just filler.
        private val commonCatches = List(24) {
            listOf(
                "Seashell", "Putting it up to your ear, you can hear the ocean.",
                "Large Stone", "A decently sized chunk of what looks to be granite.",
                "Broken Bottle", "The smell of alcohol emanates from the opening.",
                "Fishing Hook", "Rusty and crusty. There appears to be a scorpion on it?",
                "Disfigured Fork", "This fork is bent 90 degrees and is missing a prong.",
                "Bottle Cork", "Branded with 'ABC' in faint but still visible red ink."
            )
        }.intoCatches(OysterCatchRarity.COMMON)

        // Weighting of 12; could be used as a weapon or tool.
        private val uncommonCatches = List(12) {
            listOf(
                "Pearl", "The shape is almost perfectly round, and it looks somewhat red.",
                "Shark Tooth", "About an inch in size, this thing could definitely tear flesh.",
                "Credit Card", "Belongs to a 'Nicole' something. The magnetic strip is missing."
            )
        }.intoCatches(OysterCatchRarity.UNCOMMON)

        // Weight of 8; weapons and tools.
        private val rareCatches = List(8) {
            listOf(
                "War Hammer", "Heavy and sturdy, this hammer could easily crush a skull.",
                "Double Blade", "Two sided dagger with a circular hilt and extendable blades."
            )
        }.intoCatches(OysterCatchRarity.RARE)

        // Weighting of 1; can progress the game.
        private val mythicalCatches = List(1) {
            listOf(
                "Emerald Key", "The shiny emerald on the golden key glows vibrantly.",
                "Ruby Ornament", "A disc of gold adorned with a sizeable ruby in the center.",
                "Sapphire Ring", "Smooth sapphire encircles an intricate gold ring."
            )
        }.intoCatches(OysterCatchRarity.MYTHICAL)

        // This method of weighting may be damn inefficient, but it's probably fine.
        private val catches = (commonCatches + uncommonCatches + rareCatches + mythicalCatches)

        private fun List<List<String>>.intoCatches(rarity: OysterCatchRarity): List<OysterCatch> {
            return flatten().chunked(2).map { OysterCatch(it[0], it[1], rarity) }
        }
    }
}
