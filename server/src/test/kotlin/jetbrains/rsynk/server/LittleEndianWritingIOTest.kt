package jetbrains.rsynk.server

import jetbrains.rsynk.io.BufferedLittleEndianWriter
import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class LittleEndianWritingIOTest {

    @Test
    fun `ints bytes order test`() {
        val bos = ByteArrayOutputStream()
        val writer = BufferedLittleEndianWriter(bos)
        writer.writeInt(42)
        writer.flush()

        val result = bos.toByteArray()
        Assert.assertEquals(4, result.size)
        Assert.assertArrayEquals(byteArrayOf(42, 0, 0, 0), result)
    }

    @Test
    fun `chars bytes order test`() {
        val bos = ByteArrayOutputStream()
        val writer = BufferedLittleEndianWriter(bos)
        writer.writeChar(42.toChar())
        writer.flush()

        val result = bos.toByteArray()
        Assert.assertEquals(2, result.size)
        Assert.assertArrayEquals(byteArrayOf(42, 0), result)
    }

    @Test
    fun `bytes sequences order test`() {
        val bos = ByteArrayOutputStream()
        val writer = BufferedLittleEndianWriter(bos)
        writer.writeBytes(ByteBuffer.wrap(byteArrayOf(1, 2, 42, 3, 4)))
        writer.flush()

        val result = bos.toByteArray()
        Assert.assertEquals(5, result.size)
        Assert.assertArrayEquals(byteArrayOf(1, 2, 42, 3, 4), result)
    }

    @Test
    fun `write bytes bigger than buffer size test`() {
        val bos = ByteArrayOutputStream()
        val writer = BufferedLittleEndianWriter(bos, 3)
        writer.writeBytes(ByteBuffer.wrap(byteArrayOf(1, 2, 42, 3, 4)))
        writer.flush()

        val result = bos.toByteArray()
        Assert.assertEquals(5, result.size)
        Assert.assertArrayEquals(byteArrayOf(1, 2, 42, 3, 4), result)
    }
}
