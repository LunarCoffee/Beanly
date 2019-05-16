package dev.lunarcoffee.framework.core.paginators

import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.util.concurrent.TimeUnit

class PaginationReactionListener : ListenerAdapter() {
    override fun onMessageReactionAdd(event: MessageReactionAddEvent) {
        if (event.user.isBot) {
            return
        }

        // Find corresponding paginator, and return if it doesn't exist or if the reactor is not
        // the one that requested the paginator.
        val paginator = Paginator.active[event.messageId] ?: return
        if (paginator.creator != event.user) {
            return
        }

        val cp = paginator.currentPage
        paginator.changePage(
            cp + when (event.reaction.reactionEmote.emoji) {
                PaginatorButtons.FIRST.cp -> -cp
                PaginatorButtons.JUMP_LEFT.cp -> -5
                PaginatorButtons.LEFT.cp -> -1
                PaginatorButtons.CLOSE.cp -> return paginator.close()
                PaginatorButtons.RIGHT.cp -> 1
                PaginatorButtons.JUMP_RIGHT.cp -> 5
                PaginatorButtons.LAST.cp -> paginator.totalPages - cp - 1
                else -> return
            }
        )

        // Add a delay to prevent spam from making the paginator appear choppy.
        event.reaction.removeReaction(event.user).queueAfter(450, TimeUnit.MILLISECONDS)
    }
}
