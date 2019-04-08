@file:Suppress("unused")

package beanly.groups

import beanly.consts.EMBED_COLOR
import beanly.consts.EMOJI_RADIO_BUTTON
import framework.CommandGroup
import framework.dsl.command
import framework.dsl.embed
import framework.extensions.error
import framework.extensions.send
import framework.transformers.TrInt
import kotlin.random.Random

@CommandGroup("Fun")
class FunCommands {
    fun flip() = command("flip") {
        description = "Flips coins!"
        aliases = listOf("flipcoin")

        expectedArgs = listOf(TrInt(true, 1, "times to flip"))
        execute { ctx, args ->
            val times = args.get<Int>(0)

            if (times !in 1..250) {
                ctx.error("I can't flip that number of coins!")
                return@execute
            }

            val flips = (1..times).map { if (Random.nextBoolean()) "heads" else "tails" }
            val heads = flips.count { it == "heads" }
            val tails = flips.count { it == "tails" }

            ctx.send(
                embed {
                    setTitle(
                        "$EMOJI_RADIO_BUTTON  You flipped ${if (times == 1) {
                            flips[0]
                        } else {
                            "$heads heads and $tails tails"
                        }}!"
                    )
                    setDescription(flips.toString())
                    setColor(EMBED_COLOR)
                }
            )
        }
    }
}
