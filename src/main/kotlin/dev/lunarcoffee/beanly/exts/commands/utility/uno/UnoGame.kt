package dev.lunarcoffee.beanly.exts.commands.utility.uno

import dev.lunarcoffee.framework.core.CommandContext
import net.dv8tion.jda.api.entities.User

class UnoGame(ctx: CommandContext, private val owner: User) {
    private val channel = ctx.event.channel

    val deck = UnoDeck()
    private val players = mutableListOf(UnoPlayer(this, owner))

    var isStarted = false

    fun addPlayer(user: User) {
        players += UnoPlayer(this, user)
        if (players.size == 10) {
            start(owner)
        }
    }

    // Returns whether or not the user can remove a player (they can if they are the owner of the
    // owner of the game or they are themselves).
    fun removePlayer(initiator: User, target: User): Boolean {
        if (initiator.id != owner.id && initiator.id != target.id) {
            return false
        }
        players.removeIf { it.user.id == target.id }
        return true
    }

    // Returns whether or not the user has the privileges to start the game.
    fun start(user: User): Boolean {
        if (user.id != owner.id) {
            return false
        }
        isStarted = true
        return true
    }
}
