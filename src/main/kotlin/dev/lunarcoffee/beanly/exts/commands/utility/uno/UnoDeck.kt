package dev.lunarcoffee.beanly.exts.commands.utility.uno

import dev.lunarcoffee.beanly.exts.commands.utility.uno.cards.UnoCard
import dev.lunarcoffee.beanly.exts.commands.utility.uno.cards.UnoCardType

// The deck encompasses both the deck of active active and the discard pile.
class UnoDeck {
    private val active = generateDeck()
    private val discarded = mutableListOf<UnoCard>()

    // Take the card at the top of the active deck.
    fun nextCard(): UnoCard {
        if (active.isEmpty()) {
            restartDeck()
        }
        return active.removeAt(active.size - 1)
    }

    // This is used at the beginning and at deck reshuffles..
    fun discardNext() = discarded.add(nextCard())

    fun tryDiscard(player: UnoPlayer, card: UnoCard): Boolean {
        val dTop = discarded.last()

        // Allow the play if the colors match, if the numbers or symbols match, if the card is a
        // regular wild card, or if the card is a wild draw 4 and the player has no cards that have
        // the same color as the card at the top of the discard pile.
        if (
            card.color == dTop.color
            || card.type == dTop.type
            || card.type == UnoCardType.WILD
            || card.type == UnoCardType.WILD4 && player.cards.none { it.color == dTop.color }
        ) {
            player.cards.remove(card)
            discarded += card
            return true
        }

        // Deny the play.
        return false
    }

    // If all the deck is used in play, restart the deck.
    private fun restartDeck() {
        // Top of the discard pile becomes the new discard pile.
        val newDiscarded = discarded.removeAt(discarded.size - 1)

        // Make the rest of the discard pile the new active deck.
        active.clear()
        active += discarded

        // Put the old top card as the new discard pile.
        discarded.clear()
        discarded += newDiscarded
    }

    // Shuffle a new full deck.
    private fun generateDeck() = UnoCard.defaultDeck.shuffled().toMutableList()
}
