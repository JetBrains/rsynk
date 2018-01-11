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
package jetbrains.rsynk.rsync.exitvalues

internal object RsyncExitCodes {
    val Success = 0
    val SyntaxOrUsageError = 1
    val ProtocolIncompatibility = 2
    val SelectInputFilesError = 3
    val RequestActionNotSupported = 4
    val StartingServerClientProtocolError = 5
    val SocketIOError = 10
    val FileIOError = 11
    val RsyncProtocolDataStreamError = 12
}
