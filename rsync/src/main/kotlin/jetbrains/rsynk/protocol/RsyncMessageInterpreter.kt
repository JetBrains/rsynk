package jetbrains.rsynk.protocol

import mu.KLogging
import java.net.ProtocolException

class RsyncMessageInterpreter {

    companion object: KLogging()

    fun decode(tag: Int): RsyncMessage {

        val messageBytes = tag and 0xFFFFFF
        val decodedTag = (tag shr 24) - 7 //MPLEX_BASE

        logger.debug { "Got message (decodedFlag=$decodedTag, message=$messageBytes)" }

        return when (decodedTag) {

            0 -> RsyncMessage.Data // a flag for rsync to make io multiplexed and expect data

            10 -> throw UnsupportedOperationException("Unsupported tag: Stats") //RsyncMessage.Stats

            9 -> throw UnsupportedOperationException("Unsupported tag: Redo") //RsyncMessage.Redo

            22 -> throw ProtocolException("I/O error tag received") // means client experienced I/O error

            101 -> RsyncMessage.SuccessfulFileDeletion

            100 -> RsyncMessage.SuccessfulIndexUpdate

            86 -> throw ProtocolException("Received synchronize-an-error-exit from client")

            else -> throw ProtocolException("Cannot interpret tag=$tag(decoded=$decodedTag) as  a message")

        }
    }
}

sealed class RsyncMessage {
    object Data : RsyncMessage()
    object Stats : RsyncMessage()
    object Redo : RsyncMessage()
    object SuccessfulFileDeletion : RsyncMessage()
    object SuccessfulIndexUpdate : RsyncMessage()
}
