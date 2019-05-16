package dev.lunarcoffee.beanly.exts.commands.utility

import java.math.BigInteger
import java.util.*

class FastFactorialCalculator {
    private val cache = WeakHashMap<Long, BigInteger>()

    fun factorial(n: Long): BigInteger {
        return if (cache.containsKey(n)) cache[n]!! else fastFactorial(1, n).also { cache[n] = it }
    }

    // Relatively fast swing factorial algorithm.
    private fun fastFactorial(start: Long, n: Long): BigInteger {
        var i: Long
        if (n <= 16) {
            var r = BigInteger.valueOf(start)
            i = start + 1
            while (i < start + n) {
                r *= i.toBigInteger()
                i++
            }
            return r
        }
        i = n / 2
        return fastFactorial(start, i) * fastFactorial(start + i, n - i)
    }
}
