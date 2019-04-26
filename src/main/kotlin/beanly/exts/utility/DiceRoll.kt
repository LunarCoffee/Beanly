package beanly.exts.utility

private val MATCH_REGEX = """(?<times>\d*)d(?<sides>\d+)(?<mod>[+-]\d+)?""".toRegex()

class DiceRoll(val times: Int, val sides: Int, val mod: Int)

fun String.toDiceRoll(): DiceRoll {
    val (times, sides, mod) = MATCH_REGEX.matchEntire(this)?.destructured
        ?: throw IllegalArgumentException()

    return DiceRoll(
        if (times.isBlank()) 1 else times.toInt(),
        sides.toInt(),
        if (mod.isBlank()) 0 else mod.toInt()
    )
}
