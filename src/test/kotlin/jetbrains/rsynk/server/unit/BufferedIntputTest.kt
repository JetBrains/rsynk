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

class BufferedIntputTest {
    // TODO: reimplement those with messages in mind
    /*
    @Test
    fun read_a_byte_test() {
        val bos = ByteArrayOutputStream()
        bos.write(byteArrayOf(1, 2, 3, 4, 5))
        val reader = RsyncD(ByteArrayInputStream(bos.toByteArray()))

        val one = reader.readBytes(1)
        Assert.assertArrayEquals(byteArrayOf(1), one)

        val two = reader.readBytes(1)
        Assert.assertArrayEquals(byteArrayOf(2), two)
    }

    @Test
    fun read_N_bytes_test() {
        val bos = ByteArrayOutputStream()
        bos.write(byteArrayOf(1, 2, 3, 4, 5))
        val reader = RsyncDataInputImpl(ByteArrayInputStream(bos.toByteArray()))

        val read = reader.readBytes(4)
        Assert.assertArrayEquals(byteArrayOf(1, 2, 3, 4), read)
    }

    @Test
    fun read_lit_end_int_zero_test() {
        val bos = ByteArrayOutputStream()
        bos.write(byteArrayOf(0, 0, 0, 0, 0))
        val reader = RsyncDataInputImpl(ByteArrayInputStream(bos.toByteArray()))

        val read = reader.readInt()
        Assert.assertEquals(0, read)
    }

    @Test
    fun read_lit_end_max_int_test() {
        val bos = ByteArrayOutputStream()
        bos.write(byteArrayOf(-1, -1, -1, 127))
        val reader = RsyncDataInputImpl(ByteArrayInputStream(bos.toByteArray()))

        val read = reader.readInt()
        Assert.assertEquals(Int.MAX_VALUE, read)
    }

    @Test
    fun read_lit_end_int_test() {
        val bos = ByteArrayOutputStream()
        bos.write(byteArrayOf(65, 117, 74, 0))
        val reader = RsyncDataInputImpl(ByteArrayInputStream(bos.toByteArray()))

        val read = reader.readInt()
        Assert.assertEquals(4879681, read)
    }

    @Test
    fun can_read_bytes_after_int_read_test() {
        val bos = ByteArrayOutputStream()
        bos.write(byteArrayOf(0, 1, 2, 3, 4, 5))
        val reader = RsyncDataInputImpl(ByteArrayInputStream(bos.toByteArray()))

        reader.readInt()
        val read = reader.readBytes(2)

        Assert.assertArrayEquals(byteArrayOf(4, 5), read)

    }

    @Test
    fun count_read_bytes_test() {
        val bos = ByteArrayOutputStream()
        bos.write(byteArrayOf(0, 1, 2, 3, 4, 5))
        val reader = RsyncDataInputImpl(ByteArrayInputStream(bos.toByteArray()))
        reader.readBytes(2)
        reader.readBytes(2)

        Assert.assertEquals(4, reader.bytesRead())
    }

    @Test
    fun read_lit_end_char_test() {
        val bos = ByteArrayOutputStream()
        bos.write(byteArrayOf(97, 0, 98, 0, 99, 0))
        val reader = RsyncDataInputImpl(ByteArrayInputStream(bos.toByteArray()))

        Assert.assertEquals('a', reader.readChar())
        reader.readBytes(2)
        Assert.assertEquals('c', reader.readChar())
    }
    */
}
