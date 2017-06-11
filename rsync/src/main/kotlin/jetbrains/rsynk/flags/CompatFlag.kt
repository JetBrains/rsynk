/**
 * Copyright 2016 - 2017 JetBrains s.r.o.
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
package jetbrains.rsynk.flags


sealed class CompatFlag(val value: Int) {
    object IncRecurse : CompatFlag(1)
    object SymlinkTimes : CompatFlag(2)
    object SymlinkIconv : CompatFlag(4)
    object SafeFileList : CompatFlag(8)
    object AvoidFileAttributesOptimization : CompatFlag(16)
    object FixChecksumSeed : CompatFlag(32)
}

fun Set<CompatFlag>.encode(): Byte {
    return this.fold(0, { value, flag -> value.or(flag.value) }).toByte()
}

fun Byte.decodeCompatFlags(): Set<CompatFlag> {
    val thisIntValue = this.toInt()
    return listOf(CompatFlag.IncRecurse,
            CompatFlag.SymlinkTimes,
            CompatFlag.SymlinkIconv,
            CompatFlag.SafeFileList,
            CompatFlag.AvoidFileAttributesOptimization,
            CompatFlag.FixChecksumSeed)
            .filter { flag -> thisIntValue.and(flag.value) == flag.value }
            .toSet()
}
