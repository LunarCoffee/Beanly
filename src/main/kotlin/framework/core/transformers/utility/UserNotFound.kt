package framework.core.transformers.utility

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User

class UserNotFound(val jda: JDA) : User {
    override fun getDefaultAvatarId() = ""
    override fun getMutualGuilds() = emptyList<Guild>()
    override fun isBot() = false
    override fun getDefaultAvatarUrl() = ""
    override fun getName() = ""
    override fun hasPrivateChannel() = false
    override fun getJDA() = jda
    override fun getIdLong() = 0L
    override fun openPrivateChannel() = jda.selfUser.openPrivateChannel()
    override fun isFake() = false
    override fun getAsMention() = ""
    override fun getAvatarId() = ""
    override fun getDiscriminator() = ""
    override fun getAsTag() = ""
    override fun getAvatarUrl() = ""
    override fun getEffectiveAvatarUrl() = ""
}
