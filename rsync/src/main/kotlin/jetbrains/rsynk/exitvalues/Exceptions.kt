package jetbrains.rsynk.exitvalues

import jetbrains.rsynk.exitvalues.RsyncExitCodes.RsyncProtocolDataStreamError
import jetbrains.rsynk.exitvalues.RsyncExitCodes.SelectInputFilesError
import jetbrains.rsynk.exitvalues.RsyncExitCodes.ProtocolIncompatibility
import jetbrains.rsynk.exitvalues.RsyncExitCodes.RequestActionNotSupported

open class RsynkException(message: String, val exitCode: Int) : RuntimeException(message)

class ProtocolException(message: String, exitCode: Int = RsyncProtocolDataStreamError) :
        RsynkException(message, exitCode)

class ArgsParingException(message: String, exitCode: Int = RsyncProtocolDataStreamError) :
        RsynkException(message, exitCode)

class UnsupportedProtocolException(message: String) :
        RsynkException(message, ProtocolIncompatibility)

class ModuleNotFoundException(message: String) :
        RsynkException(message, SelectInputFilesError)

class InvalidFileException(message: String) :
        RsynkException(message, SelectInputFilesError)

class NotSupportedException(message: String) :
        RsynkException(message, RequestActionNotSupported)
