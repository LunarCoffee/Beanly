package framework.transformers

import java.math.BigInteger

class TrBigInt(
    override val optional: Boolean = false,
    override val default: BigInteger = BigInteger.ZERO,
    override val name: String = "number"
) : Transformer<BigInteger> {

    override fun transform(args: MutableList<String>, taken: MutableList<String>): BigInteger {
        return if (optional && args.firstOrNull()?.toBigIntegerOrNull() == null) {
            default
        } else {
            args[0].toBigInteger().also {
                taken += args.removeAt(0)
            }
        }
    }

    override fun toString() = name
}
