package framework

fun <T> silence(f: () -> T): T? {
    return try {
        f()
    } catch (e: Exception) {
        null
    }
}
