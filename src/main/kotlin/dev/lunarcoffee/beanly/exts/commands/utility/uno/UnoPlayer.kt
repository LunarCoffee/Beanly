package dev.lunarcoffee.beanly.exts.commands.utility.uno

import dev.lunarcoffee.beanly.exts.commands.utility.uno.cards.UnoCard
import dev.lunarcoffee.framework.api.dsl.embed
import dev.lunarcoffee.framework.api.extensions.await
import dev.lunarcoffee.framework.api.extensions.send
import net.dv8tion.jda.api.entities.User

class UnoPlayer(val game: UnoGame, val user: User) {
    val cards = mutableListOf<UnoCard>()

    suspend fun viewCards() {
        user.openPrivateChannel().await().send(
            embed {
                title = "your cards"
                description = cards.joinToString("\n") {
                    "**${it.color.name}** ${it.type.name}"
                }
            }
        )
    }
}
