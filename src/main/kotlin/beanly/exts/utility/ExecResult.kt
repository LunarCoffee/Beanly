package beanly.exts.utility

class ExecResult(
    val header: String,
    val stdout: String,
    val stderr: String,
    val result: Any?,
    val time: Long
) {
    companion object {
        val ERROR = ExecResult("", "", "", null, -1)
    }
}
