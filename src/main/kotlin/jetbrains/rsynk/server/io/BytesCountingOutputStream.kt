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

import java.io.OutputStream
import java.util.concurrent.atomic.AtomicLong

class BytesCountingOutputStream(
        private val host: OutputStream
) : OutputStream() {

    private val bytesWrittenCounter = AtomicLong(0)

    val bytesWritten: Long
        get() = bytesWrittenCounter.get()

    override fun write(b: Int) {
        bytesWrittenCounter.incrementAndGet()
        host.write(b)
    }

    override fun write(b: ByteArray,
                       off: Int,
                       len: Int) {
        bytesWrittenCounter.addAndGet(len.toLong())
        host.write(b, off, len)
    }

    override fun flush() {
        host.flush()
    }

    override fun close() {
        host.close()
    }
}
