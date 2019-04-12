@file:Suppress("unused")

package beanly.exts

import beanly.consts.EMOJI_BILLIARD_BALL
import beanly.consts.EMOJI_GAME_DIE
import beanly.consts.EMOJI_RADIO_BUTTON
import beanly.consts.EMOJI_THINKING
import beanly.exts.utility.DiceRoll
import beanly.exts.utility.toDiceRoll
import beanly.trimToDescription
import framework.CommandGroup
import framework.dsl.command
import framework.dsl.embed
import framework.extensions.error
import framework.extensions.send
import framework.transformers.TrGreedy
import framework.transformers.TrInt
import framework.transformers.TrSplit
import kotlin.random.Random

@CommandGroup("Fun")
class FunCommands {
    fun flip() = command("flip") {
        description = "Flips coins."
        aliases = listOf("coin", "flipcoin")

        extDescription = """
            |`flip [times]`\n
            |If an argument is provided, this command flips `times` coins, displaying all of the
            |flip results. If no argument is provided, this command will flip one coin.
        """.trimToDescription()

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
        description = "Rolls dice with RPG style roll specifiers."
        aliases = listOf("dice", "rolldice")

        extDescription = """
            |`roll [roll specs...]`\n
            |Rolls dice according to roll specifiers. Some examples are:\n
            | - `d6`: rolls a six-sided die\n
            | - `2d8`: rolls two eight-sided dice\n
            | - `d20+1`: rolls a twenty-sided die and adds one to the result\n
            | - `3d4-2`: rolls three four-sided dice and subtracts two from the result\n
            |If no specifiers are provided, a single `d6` is used.
        """.trimToDescription()

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
                    title = "$EMOJI_GAME_DIE  You rolled a ${if (diceRolls.size != 1) {
                        "total of "
                    } else {
                        ""
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

    fun pick() = command("pick") {
        description = "Picks a value from the options you give."
        aliases = listOf("select", "choose")

        extDescription = """
            |`pick options...`\n
            |Picks a value from `options`, which is a list of choices separated by `|` surrounded
            |by spaces (so you can use the pipe in an option for things like `Wolfram|Alpha`).
        """.trimToDescription()

        expectedArgs = listOf(TrSplit(" | "))
        execute { ctx, args ->
            val options = args.get<List<String>>(0)

            if (options.size < 2) {
                ctx.error("I need at least 2 options to choose from!")
                return@execute
            }

            val choice = options.random()
            ctx.send(
                embed {
                    title = "$EMOJI_THINKING  I choose **$choice**!"
                    description = options.toString()
                }
            )
        }
    }

    fun eightBall() = command("8ball") {
        val responses = listOf(
            "It is certain.",
            "It is decidedly so.",
            "Without a doubt.",
            "Yes - definitely.",
            "You may rely on it.",
            "As I see it, yes.",
            "Most likely.",
            "Outlook good.",
            "Yes.",
            "Signs point to yes.",
            "Reply hazy, try again.",
            "Ask again later.",
            "Better not tell you now.",
            "Cannot predict now.",
            "Concentrate and ask again.",
            "Don't count on it.",
            "My reply is no.",
            "My sources say no.",
            "Outlook not so good.",
            "Very doubtful."
        )

        description = "Uncover secrets with the 100% reliable Magic 8 Ball!"
        aliases = listOf("magiceightball")

        execute { ctx, _ ->
            ctx.send(
                embed {
                    title = "$EMOJI_BILLIARD_BALL  The 8-ball says:"
                    description = responses.random()
                }
            )
        }
    }
}
