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
package jetbrains.rsynk.server

class BufferedOutputTest {
    // TODO: reimplement those with messages in mind

    /*
    @Test
    fun ints_bytes_little_endian_order_test() {
        val bos = ByteArrayOutputStream()
        val writer = RsyncBufferedDataOutput(bos)
        writer.writeInt(42)
        writer.flush()

        val result = bos.toByteArray()
        Assert.assertEquals(4, result.size)
        Assert.assertArrayEquals(byteArrayOf(42, 0, 0, 0), result)
    }

    @Test
    fun chars_bytes_little_endian_order_test() {
        val bos = ByteArrayOutputStream()
        val writer = RsyncBufferedDataOutput(bos)
        writer.writeChar(42.toChar())
        writer.flush()

        val result = bos.toByteArray()
        Assert.assertEquals(2, result.size)
        Assert.assertArrayEquals(byteArrayOf(42, 0), result)
    }

    @Test
    fun bytes_sequences_little_endian_order_test() {
        val bos = ByteArrayOutputStream()
        val writer = RsyncBufferedDataOutput(bos)
        writer.writeBytes(ByteBuffer.wrap(byteArrayOf(1, 2, 42, 3, 4)))
        writer.flush()

        val result = bos.toByteArray()
        Assert.assertEquals(5, result.size)
        Assert.assertArrayEquals(byteArrayOf(1, 2, 42, 3, 4), result)
    }

    @Test
    fun write_bytes_bigger_than_buffer_size_test() {
        val bos = ByteArrayOutputStream()
        val writer = RsyncBufferedDataOutput(bos, 3)
        writer.writeBytes(ByteBuffer.wrap(byteArrayOf(1, 2, 42, 3, 4)))
        writer.flush()

        val result = bos.toByteArray()
        Assert.assertEquals(5, result.size)
        Assert.assertArrayEquals(byteArrayOf(1, 2, 42, 3, 4), result)
    }

    @Test
    fun write_bytes_bigger_than_buffer_size_portioned_test() {
        val bos = ByteArrayOutputStream()
        val writer = RsyncBufferedDataOutput(bos, 3)

        writer.writeBytes(ByteBuffer.wrap(byteArrayOf(1, 2)))
        writer.writeBytes(ByteBuffer.wrap(byteArrayOf(3, 4)))
        writer.writeBytes(ByteBuffer.wrap(byteArrayOf(5, 6, 7, 8)))
        writer.flush()

        val result = bos.toByteArray()
        Assert.assertEquals(8, result.size)
        Assert.assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8), result)
    }

    @Test
    fun write_int_byte_and_bytes_test() {
        val bos = ByteArrayOutputStream()
        val writer = RsyncBufferedDataOutput(bos, 100)

        writer.writeInt(31)
        writer.writeByte(42)
        writer.writeInt(1865987517)
        writer.writeBytes(ByteBuffer.wrap(byteArrayOf(1, 0, 0, 7)))
        writer.flush()
        Assert.assertArrayEquals(byteArrayOf(31, 0, 0, 0, 42, -67, -75, 56, 111, 1, 0, 0, 7), bos.toByteArray())
    }
    */
}
