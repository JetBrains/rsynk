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
package jetbrains.rsynk.server.io

import java.io.InputStream
import java.util.concurrent.atomic.AtomicLong

internal class BytesCountingInputStream(
        private val host: InputStream
) : InputStream() {

    private val bytesReadCounter = AtomicLong(0)

    val bytesRead: Long
        get() = bytesReadCounter.get()

    override fun read(): Int {
        bytesReadCounter.incrementAndGet()
        return host.read()
    }

    override fun read(b: ByteArray,
                      off: Int,
                      len: Int): Int {
        bytesReadCounter.addAndGet(len.toLong())
        return host.read(b, off, len)
    }

    override fun available(): Int {
        return host.available()
    }

    override fun close() {
        host.close()
    }
}
