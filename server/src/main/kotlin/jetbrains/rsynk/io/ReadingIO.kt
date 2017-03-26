package jetbrains.rsynk.io

interface ReadingIO {
    fun readBytes(len: Int): ByteArray
    val bytesRead: Long
}
