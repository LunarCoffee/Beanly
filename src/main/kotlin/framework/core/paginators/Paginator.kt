package framework.core.paginators

import framework.api.extensions.await
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.User
import java.util.*
import kotlin.concurrent.schedule

abstract class Paginator {
    abstract var message: Message
    abstract val creator: User

    abstract val pages: MutableList<Message>
    abstract var currentPage: Int
    val totalPages get() = pages.size

    abstract val closeTimer: Timer
    abstract var closeTask: TimerTask

    // This method should return a [Message] in the way it should be represented on the page.
    abstract fun formatMessage(): Message

    fun changePage(page: Int) {
        currentPage = wrapPageNumber(page)
        message.editMessage(formatMessage()).queue()

        // Reschedule the self close timeout.
        closeTask.cancel()
        closeTask = closeTimer.schedule(CLOSE_TIMEOUT) { close() }
    }

    suspend fun send(channel: MessageChannel) {
        if (totalPages == 1) {
            channel.sendMessage(formatMessage()).await()
            return
        }

        message = channel.sendMessage(formatMessage()).await()
        active[message.id] = this

        // Add buttons.
        for (button in PaginatorButtons.values()) {
            if (totalPages < 6 && button in JUMP_BUTTONS) {
                continue
            }
            message.addReaction(button.cp).queue()
        }

        // Schedule a timeout for the paginator to close itself to prevent resource leaks.
        closeTask = closeTimer.schedule(CLOSE_TIMEOUT) { close() }
    }

    fun close() {
        message.clearReactions().queue()
        Paginator.active.remove(message.id)
        closeTask.cancel()
        closeTimer.cancel()
    }

    private fun wrapPageNumber(page: Int): Int {
        return when {
            page >= pages.size -> page % pages.size
            page < 0 -> pages.size + page
            else -> page
        }
    }

    companion object {
        // Length of time that a paginator must go unused in order for it to close itself.
        const val CLOSE_TIMEOUT = 120_000L
        val JUMP_BUTTONS = arrayOf(PaginatorButtons.JUMP_LEFT, PaginatorButtons.JUMP_RIGHT)

        // Maps message IDs to their paginators.
        val active = mutableMapOf<String, Paginator>()
    }
}
