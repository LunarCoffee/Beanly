package framework.core

fun <T> silence(f: () -> T): T? {
    return try {
        f()
    } catch (e: Exception) {
        null
    }
}
