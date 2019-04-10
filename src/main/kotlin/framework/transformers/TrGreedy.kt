package framework.transformers

class TrGreedy<T>(
    val conversionFunction: (String) -> T,
    override val default: List<T> = emptyList(),
    override val name: String = "greedy"
) : Transformer<List<T>> {

    // Greedy is always technically optional, since it can steal at least 0 args.
    override val optional = true

    override fun transform(args: MutableList<String>, taken: MutableList<String>): List<T> {
        if (args.isEmpty()) {
            return default
        }

        val result = mutableListOf<T>()
        var numTaken = 0

        for (arg in args) {
            try {
                val item = args[0 + numTaken++]
                result.add(conversionFunction(item))
                taken += item
            } catch (e: Exception) {
                break
            }
        }
        args.removeAll(args.take(numTaken - 1))

        return if (result.isEmpty()) {
            default
        } else {
            result
        }
    }

    override fun toString() = name
}
