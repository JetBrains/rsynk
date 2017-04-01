package jetbrains.rsynk.exitvalues

object RsyncExitCodes {
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
