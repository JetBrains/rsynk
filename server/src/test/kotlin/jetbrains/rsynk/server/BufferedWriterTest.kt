package jetbrains.rsynk.server

import jetbrains.rsynk.io.BufferedWriter
import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class BufferedWriterTest {

    @Test
    fun ints_bytes_little_endian_order_test() {
        val bos = ByteArrayOutputStream()
        val writer = BufferedWriter(bos)
        writer.writeInt(42)
        writer.flush()

        val result = bos.toByteArray()
        Assert.assertEquals(4, result.size)
        Assert.assertArrayEquals(byteArrayOf(42, 0, 0, 0), result)
    }

    @Test
    fun chars_bytes_little_endian_order_test() {
        val bos = ByteArrayOutputStream()
        val writer = BufferedWriter(bos)
        writer.writeChar(42.toChar())
        writer.flush()

        val result = bos.toByteArray()
        Assert.assertEquals(2, result.size)
        Assert.assertArrayEquals(byteArrayOf(42, 0), result)
    }

    @Test
    fun bytes_sequences_little_endian_order_test() {
        val bos = ByteArrayOutputStream()
        val writer = BufferedWriter(bos)
        writer.writeBytes(ByteBuffer.wrap(byteArrayOf(1, 2, 42, 3, 4)))
        writer.flush()

        val result = bos.toByteArray()
        Assert.assertEquals(5, result.size)
        Assert.assertArrayEquals(byteArrayOf(1, 2, 42, 3, 4), result)
    }

    @Test
    fun write_bytes_bigger_than_buffer_size_test() {
        val bos = ByteArrayOutputStream()
        val writer = BufferedWriter(bos, 3)
        writer.writeBytes(ByteBuffer.wrap(byteArrayOf(1, 2, 42, 3, 4)))
        writer.flush()

        val result = bos.toByteArray()
        Assert.assertEquals(5, result.size)
        Assert.assertArrayEquals(byteArrayOf(1, 2, 42, 3, 4), result)
    }

    @Test
    fun write_bytes_bigger_than_buffer_size_portioned_test() {
        val bos = ByteArrayOutputStream()
        val writer = BufferedWriter(bos, 3)

        writer.writeBytes(ByteBuffer.wrap(byteArrayOf(1, 2)))
        writer.writeBytes(ByteBuffer.wrap(byteArrayOf(3, 4)))
        writer.writeBytes(ByteBuffer.wrap(byteArrayOf(5, 6, 7, 8)))
        writer.flush()

        val result = bos.toByteArray()
        Assert.assertEquals(8, result.size)
        Assert.assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8), result)
    }
}
