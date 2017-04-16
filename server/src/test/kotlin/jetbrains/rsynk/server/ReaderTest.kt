package jetbrains.rsynk.server

import jetbrains.rsynk.io.BasicReadingIO
import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class ReaderTest {

    @Test
    fun read_a_byte_test() {
        val bos = ByteArrayOutputStream()
        bos.write(byteArrayOf(1, 2, 3, 4, 5))
        val reader = BasicReadingIO(ByteArrayInputStream(bos.toByteArray()))

        val one = reader.readBytes(1)
        Assert.assertArrayEquals(byteArrayOf(1), one)

        val two = reader.readBytes(1)
        Assert.assertArrayEquals(byteArrayOf(2), two)
    }

    @Test
    fun read_N_bytes_test() {
        val bos = ByteArrayOutputStream()
        bos.write(byteArrayOf(1, 2, 3, 4, 5))
        val reader = BasicReadingIO(ByteArrayInputStream(bos.toByteArray()))

        val read = reader.readBytes(4)
        Assert.assertArrayEquals(byteArrayOf(1, 2, 3, 4), read)
    }

    @Test
    fun read_lit_end_int_zero_test() {
        val bos = ByteArrayOutputStream()
        bos.write(byteArrayOf(0, 0, 0, 0, 0))
        val reader = BasicReadingIO(ByteArrayInputStream(bos.toByteArray()))

        val read = reader.readInt()
        Assert.assertEquals(0, read)
    }

    @Test
    fun read_lit_end_max_int_test() {
        val bos = ByteArrayOutputStream()
        bos.write(byteArrayOf(-1, -1, -1, 127))
        val reader = BasicReadingIO(ByteArrayInputStream(bos.toByteArray()))

        val read = reader.readInt()
        Assert.assertEquals(Int.MAX_VALUE, read)
    }

    @Test
    fun read_lit_end_int_test() {
        val bos = ByteArrayOutputStream()
        bos.write(byteArrayOf(65, 117, 74, 0))
        val reader = BasicReadingIO(ByteArrayInputStream(bos.toByteArray()))

        val read = reader.readInt()
        Assert.assertEquals(4879681, read)
    }

    @Test
    fun can_read_bytes_after_int_read_test() {
        val bos = ByteArrayOutputStream()
        bos.write(byteArrayOf(0, 1, 2, 3, 4, 5))
        val reader = BasicReadingIO(ByteArrayInputStream(bos.toByteArray()))

        reader.readInt()
        val read = reader.readBytes(2)

        Assert.assertArrayEquals(byteArrayOf(4, 5), read)

    }

    @Test
    fun count_read_bytes_test() {
        val bos = ByteArrayOutputStream()
        bos.write(byteArrayOf(0, 1, 2, 3, 4, 5))
        val reader = BasicReadingIO(ByteArrayInputStream(bos.toByteArray()))
        reader.readBytes(2)
        reader.readBytes(2)

        Assert.assertEquals(4, reader.bytesRead())
    }

    @Test
    fun read_lit_end_char_test() {
        val bos = ByteArrayOutputStream()
        bos.write(byteArrayOf(97, 0, 98, 0, 99, 0))
        val reader = BasicReadingIO(ByteArrayInputStream(bos.toByteArray()))

        Assert.assertEquals('a', reader.readChar())
        reader.readBytes(2)
        Assert.assertEquals('c', reader.readChar())
    }
}
