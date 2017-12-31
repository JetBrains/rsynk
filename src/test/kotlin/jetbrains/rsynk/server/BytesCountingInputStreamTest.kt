package jetbrains.rsynk.server

import jetbrains.rsynk.server.io.BytesCountingInputStream
import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

class BytesCountingInputStreamTest {

    @Test
    fun delegate_input_stream_read_methods_test() {
        val bis = ByteArrayInputStream(ByteArray(50, { i -> i.toByte() }))
        val bytesCountingStream = BytesCountingInputStream(bis)

        val firstByte = bytesCountingStream.read()
        Assert.assertEquals(0, firstByte)

        val buf = ByteArray(20)
        bytesCountingStream.read(buf)
        Assert.assertArrayEquals(ByteArray(20, { i -> (i + 1).toByte() }), buf)

        buf.map { 0 }
        bytesCountingStream.read(buf, 1, 1)
        Assert.assertEquals(21.toByte(), buf[1])
    }

    @Test
    fun delegate_available_method_test() {
        val bis = ByteArrayInputStream(ByteArray(50))
        val bytesCountingStream = BytesCountingInputStream(bis)

        bytesCountingStream.read(ByteArray(30))
        Assert.assertEquals(20, bytesCountingStream.available())
    }

    @Test
    fun delegate_close_method_test() {
        val testCloseMethodInputStream = object : InputStream() {
            private var closed = false
            override fun read(): Int {
                if (closed) {
                    return 1
                }
                return 0
            }

            override fun close() {
                closed = true
            }
        }

        val bytesCountingStream = BytesCountingInputStream(testCloseMethodInputStream)
        Assert.assertEquals(0, bytesCountingStream.read())
        bytesCountingStream.close()
        Assert.assertEquals(1, bytesCountingStream.read())
    }

    @Test
    fun count_bytes_test() {
        val nullInputStream = object : InputStream() {
            override fun read(): Int = 0
        }
        val bytesCountingStream = BytesCountingInputStream(nullInputStream)

        bytesCountingStream.read()
        Assert.assertEquals(1, bytesCountingStream.bytesRead)

        bytesCountingStream.read(ByteArray(30))
        Assert.assertEquals(31, bytesCountingStream.bytesRead)

        bytesCountingStream.read(kotlin.ByteArray(30), 3, 5)
        Assert.assertEquals(36, bytesCountingStream.bytesRead)
    }
}
