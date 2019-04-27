package framework.transformers.utility

class SplitTime(val days: Long, val hours: Long, val minutes: Long, val seconds: Long) {
    companion object {
        val NONE = SplitTime(-1, -1, -1, -1)
    }
}
