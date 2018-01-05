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

import jetbrains.rsynk.server.io.RsyncTaggingInput
import jetbrains.rsynk.server.io.RsyncTaggingOutput
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
