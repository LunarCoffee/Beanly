package framework.transformers

class TrDouble(
    override val optional: Boolean = false,
    override val default: Double = 0.0,
    override val name: String = "decimal"
) : Transformer<Double> {

    override fun transform(args: MutableList<String>, taken: MutableList<String>): Double {
        return if (optional && args.firstOrNull()?.toDoubleOrNull() == null) {
            default
        } else {
            args[0].toDouble().also {
                taken += args.removeAt(0)
            }
        }
    }

    override fun toString() = name
}
