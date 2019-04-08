package framework.transformers

interface Transformer<T> {
    val optional: Boolean
    val default: T
    val name: String

    fun transform(args: MutableList<String>, taken: MutableList<String>): T

    override fun toString(): String
}
