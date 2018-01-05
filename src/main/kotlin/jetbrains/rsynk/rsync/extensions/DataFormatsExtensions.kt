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
package jetbrains.rsynk.rsync.extensions

val Byte.Companion.MAX_VALUE_UNSIGNED: Int
    get() = Byte.MAX_VALUE * 2 + 1

fun Int.toLittleEndianBytes(): ByteArray {
    return byteArrayOf(this.ushr(0).toByte(),
            this.ushr(8).toByte(),
            this.ushr(16).toByte(),
            this.ushr(24).toByte())
}
