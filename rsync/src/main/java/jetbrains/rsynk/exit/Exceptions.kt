package jetbrains.rsynk.exit

import jetbrains.rsynk.exit.RsyncExitCodes.ERROR_IN_RSYNC_PROTOCOL_DATA_STREAM
import jetbrains.rsynk.exit.RsyncExitCodes.ERROR_SELECTING_INPUT_FILES
import jetbrains.rsynk.exit.RsyncExitCodes.PROTOCOL_INCOMPATIBILITY
import jetbrains.rsynk.exit.RsyncExitCodes.REQUESTED_ACTION_NOT_SUPPORTED

open class RsynkException(message: String, val exitCode: Int) : RuntimeException(message)

class ProtocolException(message: String, exitCode: Int = ERROR_IN_RSYNC_PROTOCOL_DATA_STREAM) :
        RsynkException(message, exitCode)

class UnsupportedProtocolException(message: String) :
        RsynkException(message, PROTOCOL_INCOMPATIBILITY)

class ModuleNotFoundException(message: String) :
        RsynkException(message, ERROR_SELECTING_INPUT_FILES)

class ActionNotSupportedException(message: String) :
        RsynkException(message, REQUESTED_ACTION_NOT_SUPPORTED)