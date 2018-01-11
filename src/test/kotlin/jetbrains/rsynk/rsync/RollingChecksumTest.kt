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
package jetbrains.rsynk.rsync

import jetbrains.rsynk.rsync.data.RollingChecksum
import org.junit.Assert
import org.junit.Test

internal class RollingChecksumTest {

    @Test
    fun rolling_checksum_test_1() {
        val bytes = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        Assert.assertEquals(10813485, RollingChecksum.calculate(bytes, 0, 10))
    }

    @Test
    fun rolling_checksum_test_2() {
        val bytes = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        Assert.assertEquals(6225955, RollingChecksum.calculate(bytes, 5, 5))
    }

    @Test
    fun rolling_checksum_test_3() {
        val bytes = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        Assert.assertEquals(458757, RollingChecksum.calculate(bytes, 2, 2))
    }

    @Test
    fun rolling_checksum_test_4() {
        val len = 10000
        val bytes = ByteArray(len)
        for(i in 0..len - 1) {
            bytes[i] = (i * 2).toByte()
        }
        Assert.assertEquals(139516400, RollingChecksum.calculate(bytes, 0, len))
    }

    @Test
    fun roll_forward_test_1() {
        Assert.assertEquals(-499785164, RollingChecksum.rollForward(123321, 123.toByte()))
    }

    @Test
    fun roll_forward_test_2() {
        Assert.assertEquals(-737291240, RollingChecksum.rollForward(-666666, 66.toByte()))
    }

    @Test
    fun roll_forward_test_3() {
        Assert.assertEquals(10551457, RollingChecksum.rollForward(7 * 6 + 42, 77.toByte()))
    }

    @Test
    fun roll_back_test_1() {
        Assert.assertEquals(-170293113, RollingChecksum.rollBack(100500, 200, 13.toByte()))
    }

    @Test
    fun roll_back_test_2() {
        Assert.assertEquals(-115603782, RollingChecksum.rollBack(42 * 42, 42, 42.toByte()))
    }

    @Test
    fun roll_back_test_3() {
        Assert.assertEquals(1707285964, RollingChecksum.rollBack(-123321, 321, 123.toByte()))
    }
}
