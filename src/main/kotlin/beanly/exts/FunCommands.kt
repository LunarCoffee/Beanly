@file:Suppress("unused")

package beanly.exts

import beanly.consts.EMOJI_GAME_DIE
import beanly.consts.EMOJI_RADIO_BUTTON
import beanly.exts.utility.DiceRoll
import beanly.exts.utility.toDiceRoll
import framework.CommandGroup
import framework.dsl.command
import framework.dsl.embed
import framework.extensions.error
import framework.extensions.send
import framework.transformers.TrGreedy
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

            if (times !in 1..292) {
                ctx.error("I can't flip that number of coins!")
                return@execute
            }

            val flips = (1..times).map { if (Random.nextBoolean()) "heads" else "tails" }
            val heads = flips.count { it == "heads" }
            val tails = flips.count { it == "tails" }

            ctx.send(
                embed {
                    title = "$EMOJI_RADIO_BUTTON  You flipped ${if (times == 1) {
                        flips[0]
                    } else {
                        "$heads heads and $tails tails"
                    }}!"
                    description = flips.toString()
                }
            )
        }
    }

    fun roll() = command("roll") {
        expectedArgs = listOf(TrGreedy(String::toDiceRoll, listOf(DiceRoll(1, 6, 0))))
        execute { ctx, args ->
            val diceRolls = args.get<List<DiceRoll>>(0)

            if (diceRolls.any { it.times > 1000 || it.sides > 1000 || it.mod !in -10000..10000 }) {
                ctx.error("At least one of your rolls had a number that was too big or small!")
                return@execute
            }

            val results = diceRolls
                .map { roll -> List(roll.times) { Random.nextInt(1, roll.sides) } }

            // Sum all the results of each individual roll and add all the modifiers.
            val total = results.flatten().sum() + diceRolls.sumBy { it.mod }

            ctx.send(
                embed {
                    title = "$EMOJI_GAME_DIE  You rolled a ${if (diceRolls.size == 1) {
                        ""
                    } else {
                        "total of "
                    }}$total!"

                    description = results.zip(diceRolls).joinToString("\n") { (res, roll) ->
                        val modifierSign = if (roll.mod <= 0) "" else "+"
                        val modifier = if (roll.mod != 0) roll.mod.toString() else ""

                        val modifierAndSign = modifierSign + modifier
                        "**${roll.times}d${roll.sides}$modifierAndSign**: $res $modifierAndSign"
                    }
                }
            )
        }
    }
}
