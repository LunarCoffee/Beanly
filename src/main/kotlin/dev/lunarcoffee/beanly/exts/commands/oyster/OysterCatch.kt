package dev.lunarcoffee.beanly.exts.commands.oyster

class OysterCatch(
    val name: String,
    val description: String,
    val rarity: OysterCatchRarity
) {
    val nameWithPrefix get() = rarity.prefix + name

    companion object {
        val EMPTY = OysterCatch("", "", OysterCatchRarity.COMMON)

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
        val catches = (commonCatches + uncommonCatches + rareCatches + mythicalCatches)

        private fun List<List<String>>.intoCatches(rarity: OysterCatchRarity): List<OysterCatch> {
            return flatten().chunked(2).map { OysterCatch(it[0], it[1], rarity) }
        }
    }
}
