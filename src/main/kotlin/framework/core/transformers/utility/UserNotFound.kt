package framework.core.transformers.utility

import net.dv8tion.jda.api.entities.User

sealed class UserSearchResult

class Found(val user: User) : UserSearchResult()
object NotFound : UserSearchResult()
