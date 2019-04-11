package framework.transformers

class TrSplit(
    private val separator: String = " ",
    override val default: List<String> = emptyList(),
    override val name: String = "split"
) : Transformer<List<String>> {

    // Split is always technically optional.
    override val optional = true

    override fun transform(args: MutableList<String>, taken: MutableList<String>): List<String> {
        return args.joinToString(" ").split(separator).also {
            taken += args
            args.clear()
        }
    }

    override fun toString() = name
}
