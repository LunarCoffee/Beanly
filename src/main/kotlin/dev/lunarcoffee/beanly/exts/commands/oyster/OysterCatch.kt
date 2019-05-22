package dev.lunarcoffee.beanly.exts.commands.oyster

class OysterCatch(
    val name: String,
    val description: String,
    val rarity: OysterCatchRarity
) {
    val nameWithPrefix get() = rarity.prefix + name

    companion object {
        val EMPTY = OysterCatch("", "", OysterCatchRarity.COMMON)
    }
}
