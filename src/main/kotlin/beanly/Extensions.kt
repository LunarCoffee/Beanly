package beanly

fun <T> Iterable<T>.ifEmptyToString(): String {
    return if (this.count() == 0) {
        "(none)"
    } else {
        toString()
    }
}
