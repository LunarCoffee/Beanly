package framework.transformers

class TrSplit(
    override val default: List<String> = emptyList(),
    override val name: String = "split"
) : Transformer<List<String>> {

    // Split is always technically optional.
    override val optional = true

    override fun transform(args: MutableList<String>, taken: MutableList<String>): List<String> {
        val temp = args.takeWhile { true }
        taken += args
        args.clear()

        return temp
    }

    override fun toString() = name
}