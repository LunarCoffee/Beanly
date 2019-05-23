package dev.lunarcoffee.beanly.exts.commands.utility.uno

import dev.lunarcoffee.beanly.consts.Emoji
import dev.lunarcoffee.framework.api.dsl.embed
import dev.lunarcoffee.framework.api.extensions.error
import dev.lunarcoffee.framework.api.extensions.send
import dev.lunarcoffee.framework.api.extensions.success
import dev.lunarcoffee.framework.core.CommandContext
import dev.lunarcoffee.framework.core.transformers.utility.Found
import dev.lunarcoffee.framework.core.transformers.utility.NotFound
import dev.lunarcoffee.framework.core.transformers.utility.UserSearchResult
import net.dv8tion.jda.api.entities.User

class UnoGame(ctx: CommandContext, private val owner: User) {
    private val channel = ctx.event.channel

    val players = mutableListOf(UnoPlayer(this, owner))
    val deck = UnoDeck()

    var curPlayerIndex = 0
    var isStarted = false

    fun addPlayer(user: User): Boolean {
        if (user.id in players.map { it.user.id }) {
            return false
        }
        players += UnoPlayer(this, user)
        return true
    }

    // TODO: Do things with the player's deck.
    // TODO: Do something if the owner of the game leaves.
    fun removePlayer(initiator: User, target: User): Boolean? {
        if (initiator.id != owner.id && initiator.id != target.id) {
            return null
        }

        val player = players.find { it.user.id == target.id } ?: return false
        deck.insertBottom(player.cards)
        players.remove(player)

        return true
    }

    // This is a helper function for use externally; don't use this internally inside the class.
    suspend fun kickPlayer(ctx: CommandContext, initiator: User, targetResult: UserSearchResult?) {
        val target = when (targetResult) {
            is Found -> targetResult.user
            NotFound -> {
                ctx.error("I can't find that user!")
                return
            }
            else -> {
                ctx.error("You need to give me a user to kick!")
                return
            }
        }

        // Make sure they're in the current game.
        if (target.id !in players.map { it.user.id }) {
            ctx.error("That user isn't in this game!")
            return
        }

        // Try to remove them; if they can't, [removePlayer] returns [false].
        val removed = removePlayer(initiator, target)
        if (removed == null) {
            ctx.error("Only the owner of the game can kick players!")
            return
        }
        ctx.success("Kicked `${target.asTag}` from the game!")
    }

    // Returns whether or not the user has the privileges to start the game.
    fun start(user: User = owner): Boolean {
        if (user.id != owner.id) {
            return false
        }
        isStarted = true
        return true
    }

    // Send all players at the table in the order of play.
    suspend fun sendTable(ctx: CommandContext) {
        ctx.send(
            embed {
                title = "${Emoji.FLOWER_PLAYING_CARDS}  The current game table:"
                description = players.mapIndexed { index, player ->
                    if (isStarted && index == curPlayerIndex) {
                        "**${player.user.asTag}**"
                    } else {
                        player.user.asTag
                    }
                }.toString()

                footer { text = "It is the bolded player's turn." }
            }
        )
    }
}
