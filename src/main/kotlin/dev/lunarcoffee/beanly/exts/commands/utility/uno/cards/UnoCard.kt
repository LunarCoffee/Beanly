package dev.lunarcoffee.beanly.exts.commands.utility.uno.cards

class UnoCard(val type: UnoCardType, val color: UnoCardColor) {
    companion object {
        // Types of UNO cards.
        private val cardTypes = UnoCardType.values().toList()

        private val fourKind = List(4) { cardTypes.filter(::isFourKind) }.flatten()
        private val eightKind = List(8) { cardTypes.filter { !isFourKind(it) } }.flatten()

        val defaultDeck = (fourKind + eightKind)
            .mapIndexed { index, type -> UnoCard(type, UnoCardColor.values()[index % 4]) }

        private fun isFourKind(c: UnoCardType): Boolean {
            return c == UnoCardType.ZERO || c == UnoCardType.WILD || c == UnoCardType.WILD4
        }
    }
}
