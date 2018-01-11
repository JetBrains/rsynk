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

import jetbrains.rsynk.rsync.exitvalues.RsyncExitCodes.ProtocolIncompatibility
import jetbrains.rsynk.rsync.exitvalues.RsyncExitCodes.RequestActionNotSupported
import jetbrains.rsynk.rsync.exitvalues.RsyncExitCodes.RsyncProtocolDataStreamError
import jetbrains.rsynk.rsync.exitvalues.RsyncExitCodes.SelectInputFilesError

internal open class RsynkException(message: String, val exitCode: Int, cause: Throwable?) : RuntimeException(message, cause)

internal class ProtocolException(message: String, exitCode: Int = RsyncProtocolDataStreamError, cause: Throwable? = null) :
        RsynkException(message, exitCode, cause)

internal class ArgsParingException(message: String, exitCode: Int = RsyncProtocolDataStreamError, cause: Throwable? = null) :
        RsynkException(message, exitCode, cause)

internal class UnsupportedProtocolException(message: String, cause: Throwable? = null) :
        RsynkException(message, ProtocolIncompatibility, cause)

internal class ModuleNotFoundException(message: String, cause: Throwable? = null) :
        RsynkException(message, SelectInputFilesError, cause)

internal class InvalidFileException(message: String, cause: Throwable? = null) :
        RsynkException(message, SelectInputFilesError, cause)

internal class NotSupportedException(message: String, cause: Throwable? = null) :
        RsynkException(message, RequestActionNotSupported, cause)
