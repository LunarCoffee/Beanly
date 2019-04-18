package beanly.exts.utility

import java.lang.management.ManagementFactory

class SystemStatistics {
    val totalMemory = Runtime.getRuntime().totalMemory() / 1_000_000
    val freeMemory = Runtime.getRuntime().freeMemory() / 1_000_000

    val uptime = ManagementFactory.getRuntimeMXBean().uptime.run {
        val days = this / 86_400_000
        val hours = this / 3_600_000 % 24
        val minutes = this / 60_000 % 60
        val seconds = this / 1000 % 60

        "$days days, $hours hours, $minutes minutes, $seconds seconds"
    }

    val cpuArchitecture = retrieveCpuArchitecture()
    val logicalProcessors = Runtime.getRuntime().availableProcessors()

    val osName = System.getProperty("os.name") ?: "(unknown)"
    val language = "Kotlin 1.3.30"
    val jvmVersion = System.getProperty("java.version") ?: "(unknown)"

    var runningThreads: Int
    val totalThreads = Thread
        .getAllStackTraces()
        .keys
        .also { threads ->
            runningThreads = threads.filter { it.state == Thread.State.RUNNABLE }.size
        }
        .size

    private fun retrieveCpuArchitecture(): String {
        return if ("Windows" in System.getProperty("os.name")) {
            val cpuArch = System.getenv("PROCESSOR_ARCHITECTURE")
            val wow64Arch = System.getenv("PROCESSOR_ARCHITEW6432")

            if (cpuArch != null && "64" in cpuArch || wow64Arch != null && "64" in wow64Arch) {
                "64-bit"
            } else {
                "32-bit"
            }
        } else {
            when (System.getProperty("os.arch")) {
                "amd64" -> "64-bit"
                "ia64" -> "Itanium 64-bit"
                "x86" -> "32-bit"
                else -> "(unknown)"
            }
        }
    }
}
