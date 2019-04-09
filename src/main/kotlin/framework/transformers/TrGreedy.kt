package framework.transformers

import java.lang.Exception

class TrGreedy<T>(
    val conversionFunction: (String) -> T,
    override val optional: Boolean = false,
    override val default: List<T> = emptyList(),
    override val name: String = "number"
) : Transformer<List<T>> {

    override fun transform(
        args: MutableList<String>,
        taken: MutableList<String>
    ): List<T> {

        // TODO: Logic is right, make this actually work.
        return args.takeWhile {
            try {
                conversionFunction(it)
                return@takeWhile true
            } catch (e: Exception) {
                return@takeWhile false
            }
        }.map { conversionFunction(it) }
    }

    override fun toString() = name
}
