package framework.transformers

import net.dv8tion.jda.api.entities.User

class TrUser(
    override val optional: Boolean = false,
    override val default: User,
    override val name: String = "user"
) : Transformer<User> {

    override fun transform(
        args: MutableList<String>,
        taken: MutableList<String>
    ): User {

        if (optional && args.isEmpty()) {
            return default
        }

        // TODO: implement lol
        return default
    }

    override fun toString() = name
}
