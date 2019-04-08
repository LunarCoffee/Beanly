package framework.transformers

class TrWord(
    override val optional: Boolean = false,
    override val default: String = "",
    override val name: String = "word"
) : Transformer<String> {

    override fun transform(
        args: MutableList<String>,
        taken: MutableList<String>
    ): String {

        return if (optional && args.isEmpty()) {
            default
        } else {
            args[0].also {
                taken += args.removeAt(0)
            }
        }
    }

    override fun toString() = name
}
