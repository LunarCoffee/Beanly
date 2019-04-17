package framework.transformers.utility

import net.dv8tion.jda.api.entities.User

class UserNotFound : User {
    override fun getDefaultAvatarId() = null
    override fun getMutualGuilds() = null
    override fun isBot() = false
    override fun getDefaultAvatarUrl() = null
    override fun getName() = null
    override fun hasPrivateChannel() = false
    override fun getJDA() = null
    override fun getIdLong() = 0L
    override fun openPrivateChannel() = null
    override fun isFake() = false
    override fun getAsMention() = null
    override fun getAvatarId() = null
    override fun getDiscriminator() = null
    override fun getAsTag() = null
    override fun getAvatarUrl() = null
    override fun getEffectiveAvatarUrl() = null
}
