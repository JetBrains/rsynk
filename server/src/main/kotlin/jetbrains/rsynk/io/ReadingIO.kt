package jetbrains.rsynk.io

interface ReadingIO {
    fun readBytes(len: Int): ByteArray

    fun readInt(): Int

    fun bytesRead(): Long
}
