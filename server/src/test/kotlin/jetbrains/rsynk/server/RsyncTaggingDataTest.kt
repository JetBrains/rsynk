package jetbrains.rsynk.server

import jetbrains.rsynk.io.RsyncTaggingInput
import jetbrains.rsynk.io.RsyncTaggingOutput
import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class RsyncTaggingDataTest {

    @Test
    fun write_min_integer_test() {
        val bos = ByteArrayOutputStream()
        val output = RsyncTaggingOutput(bos)

        output.writeInt(Integer.MIN_VALUE)
        output.flush()
        Assert.assertArrayEquals(byteArrayOf(4, 0, 0, 7, 0, 0, 0, -128), bos.toByteArray())
    }

    @Test
    fun read_min_integer_test() {
        val input = RsyncTaggingInput(ByteArrayInputStream(byteArrayOf(4, 0, 0, 7, 0, 0, 0, -128)))
        val int = input.readInt()
        Assert.assertEquals(Integer.MIN_VALUE, int)
    }
}
