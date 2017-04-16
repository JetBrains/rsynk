package jetbrains.rsynk.io

interface ReadingIO {
    fun readBytes(len: Int): ByteArray

    fun readInt(): Int

    fun readChar(): Char

    fun bytesRead(): Long
}
