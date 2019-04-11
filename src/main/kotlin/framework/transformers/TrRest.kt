package framework.transformers

class TrRest(
    override val optional: Boolean = false,
    override val default: String = "",
    override val name: String = "rest"
) : Transformer<String> {

    override fun transform(args: MutableList<String>, taken: MutableList<String>): String {
        return if (optional && args.isEmpty()) {
            default
        } else {
            args.joinToString(" ").also {
                taken += args
                args.clear()
            }
        }
    }

    override fun toString() = name
}
