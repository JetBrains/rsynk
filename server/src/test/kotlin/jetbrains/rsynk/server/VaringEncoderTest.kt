package jetbrains.rsynk.server

import jetbrains.rsynk.io.VarintEncoder
import org.junit.Assert
import org.junit.Test
import java.nio.ByteBuffer


class VarintEncoderTest {

    @Test
    fun `encode long min value test`() {
        val encoded = VarintEncoder.varlong(Long.MIN_VALUE, 3)
        Assert.assertEquals(ByteBuffer.wrap(byteArrayOf(-4, 0, 0, 0, 0, 0, 0, 0, -128)), encoded)
    }

    @Test
    fun `encode long max value test`() {
        val encoded = VarintEncoder.varlong(Long.MAX_VALUE, 3)
        Assert.assertEquals(ByteBuffer.wrap(byteArrayOf(-4, -1, -1, -1, -1, -1, -1, -1, 127)), encoded)
    }

    @Test
    fun `encode Long max value minus 1 test`() {
        val encoded = VarintEncoder.varlong(Long.MAX_VALUE - 1, 3)
        Assert.assertEquals(ByteBuffer.wrap(byteArrayOf(-4, -2, -1, -1, -1, -1, -1, -1, 127)), encoded)
    }

    @Test
    fun `encode zero length 5 test`() {
        val encoded = VarintEncoder.varlong(0L, 5)
        Assert.assertEquals(ByteBuffer.wrap(byteArrayOf(0, 0, 0, 0, 0)), encoded)
    }

    @Test
    fun `encode 1 test`() {
        val encoded = VarintEncoder.varlong(1, 3)
        Assert.assertEquals(ByteBuffer.wrap(byteArrayOf(0, 1, 0)), encoded)
    }

    @Test
    fun `encode 255 test`() {
        val encoded = VarintEncoder.varlong(255, 4)
        Assert.assertEquals(ByteBuffer.wrap(byteArrayOf(0, -1, 0, 0)), encoded)
    }

    @Test
    fun `encode 256 test`() {
        val encoded = VarintEncoder.varlong(256, 3)
        Assert.assertEquals(ByteBuffer.wrap(byteArrayOf(0, 0, 1)), encoded)
    }

    @Test
    fun `encode 2 pow 32 test`() {
        val encoded = VarintEncoder.varlong(Math.pow(2.0, 32.0).toLong(), 3)
        Assert.assertEquals(ByteBuffer.wrap(byteArrayOf(-63, 0, 0, 0, 0)), encoded)
    }
}
