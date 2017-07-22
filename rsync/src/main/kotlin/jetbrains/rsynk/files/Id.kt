package jetbrains.rsynk.files

import java.util.concurrent.atomic.AtomicLong


object Id {
    private val id = AtomicLong(0)

    fun newUniqueId(): String {
        return id.getAndIncrement().toString()
    }
}
