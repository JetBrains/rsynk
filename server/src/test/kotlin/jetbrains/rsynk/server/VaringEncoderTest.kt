package jetbrains.rsynk.server

import jetbrains.rsynk.io.VarintEncoder
import org.junit.Assert
import org.junit.Test
import java.nio.ByteBuffer


class VarintEncoderTest {

    @Test
    fun encode_long_min_value_test() {
        val encoded = VarintEncoder.varlong(Long.MIN_VALUE, 3)
        Assert.assertEquals(ByteBuffer.wrap(byteArrayOf(-4, 0, 0, 0, 0, 0, 0, 0, -128)), encoded)
    }

    @Test
    fun encode_long_max_value_test() {
        val encoded = VarintEncoder.varlong(Long.MAX_VALUE, 3)
        Assert.assertEquals(ByteBuffer.wrap(byteArrayOf(-4, -1, -1, -1, -1, -1, -1, -1, 127)), encoded)
    }

    @Test
    fun encode_Long_max_value_minus_1_test() {
        val encoded = VarintEncoder.varlong(Long.MAX_VALUE - 1, 3)
        Assert.assertEquals(ByteBuffer.wrap(byteArrayOf(-4, -2, -1, -1, -1, -1, -1, -1, 127)), encoded)
    }

    @Test
    fun encode_zero_length_5_test() {
        val encoded = VarintEncoder.varlong(0L, 5)
        Assert.assertEquals(ByteBuffer.wrap(byteArrayOf(0, 0, 0, 0, 0)), encoded)
    }

    @Test
    fun encode_1_test() {
        val encoded = VarintEncoder.varlong(1, 3)
        Assert.assertEquals(ByteBuffer.wrap(byteArrayOf(0, 1, 0)), encoded)
    }

    @Test
    fun encode_255_test() {
        val encoded = VarintEncoder.varlong(255, 4)
        Assert.assertEquals(ByteBuffer.wrap(byteArrayOf(0, -1, 0, 0)), encoded)
    }

    @Test
    fun encode_256_test() {
        val encoded = VarintEncoder.varlong(256, 3)
        Assert.assertEquals(ByteBuffer.wrap(byteArrayOf(0, 0, 1)), encoded)
    }

    @Test
    fun encode_2_power_32_test() {
        val encoded = VarintEncoder.varlong(Math.pow(2.0, 32.0).toLong(), 3)
        Assert.assertEquals(ByteBuffer.wrap(byteArrayOf(-63, 0, 0, 0, 0)), encoded)
    }
}
