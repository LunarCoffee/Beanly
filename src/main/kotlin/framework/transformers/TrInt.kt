package framework.transformers

class TrInt(
    override val optional: Boolean = false,
    override val default: Int = 0,
    override val name: String = "number"
) : Transformer<Int> {

    override fun transform(
        args: MutableList<String>,
        taken: MutableList<String>
    ): Int {

        return if (optional && args.firstOrNull()?.toIntOrNull() == null) {
            default
        } else {
            args[0].toInt().also {
                taken += args.removeAt(0)
            }
        }
    }

    override fun toString() = name
}
