package jetbrains.rsynk.server.command


interface TimerInstance {
    fun getTimeFromStart(): Long
}

private class TimerInstanceImpl(
        private val startTime: Long
) : TimerInstance {
    override fun getTimeFromStart(): Long {
        val now = System.currentTimeMillis()
        return Math.max(now - startTime, 1)
    }
}

object CommandExecutionTimer {
    fun start(): TimerInstance {
        val now = System.currentTimeMillis()
        return TimerInstanceImpl(now)
    }
}
