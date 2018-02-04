/**
 * Copyright 2016 - 2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jetbrains.rsynk.server.unit

import jetbrains.rsynk.server.io.BytesCountingOutputStream
import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.OutputStream

class BytesCountingOutputStreamTest {
    @Test
    fun delegate_output_stream_write_methods_test() {
        val bos = ByteArrayOutputStream()
        val bytesCountingStream = BytesCountingOutputStream(bos)

        bytesCountingStream.write(0)

        val buf = ByteArray(10, { i -> (i + 1).toByte() })
        bytesCountingStream.write(buf)

        buf[1] = 11
        bytesCountingStream.write(buf, 1, 1)

        Assert.assertArrayEquals(ByteArray(12, { i -> i.toByte() }), bos.toByteArray())
    }

    @Test
    fun delegate_flush_method_test() {
        val testFlushOutputStream = object : OutputStream() {
            override fun write(b: Int) = Unit

            var flushCalled = false

            override fun flush() {
                flushCalled = true
            }
        }

        BytesCountingOutputStream(testFlushOutputStream).flush()
        Assert.assertTrue(testFlushOutputStream.flushCalled)
    }

    @Test
    fun delegate_close_method_test() {
        val testFlushOutputStream = object : OutputStream() {
            override fun write(b: Int) = Unit

            var closeCalled = false

            override fun close() {
                closeCalled = true
            }
        }
        BytesCountingOutputStream(testFlushOutputStream).close()
        Assert.assertTrue(testFlushOutputStream.closeCalled)
    }

    @Test
    fun count_written_bytes_test() {
        val bytesCountingStream = BytesCountingOutputStream(object : OutputStream() {
            override fun write(b: Int) = Unit
        })

        bytesCountingStream.write(0)
        Assert.assertEquals(1, bytesCountingStream.bytesWritten)

        bytesCountingStream.write(ByteArray(20))
        Assert.assertEquals(21, bytesCountingStream.bytesWritten)

        bytesCountingStream.write(ByteArray(20), 5, 10)
        Assert.assertEquals(31, bytesCountingStream.bytesWritten)
    }
}
