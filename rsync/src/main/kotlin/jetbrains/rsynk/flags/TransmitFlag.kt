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

sealed class TransmitFlag(val value: Int) {
    object TopDirectory : TransmitFlag(1)
    object SameMode : TransmitFlag(2)
    object ExtendedFlags : TransmitFlag(4)
    object SameUserId : TransmitFlag(8)
    object SameGroupId : TransmitFlag(16)
    object SameName : TransmitFlag(32)
    object SameLongName : TransmitFlag(64)
    object SameLastModifiedTime : TransmitFlag(128)
    object SameRdevMajor : TransmitFlag(256)
    object NoContentDirs : TransmitFlag(256)
    object HardLinked : TransmitFlag(512)
    object UserNameFollows : TransmitFlag(1024)
    object GroupNameFollows : TransmitFlag(2048)
    object HardLinksFirst : TransmitFlag(4096)
    object IoErrorEndList : TransmitFlag(4096)
    object ModNsec : TransmitFlag(8192)
}
//TODO make separate encoder/decoder
fun Set<TransmitFlag>.encode(): Int {
    return this.fold(0, { value, flag -> value.or(flag.value) })
}


