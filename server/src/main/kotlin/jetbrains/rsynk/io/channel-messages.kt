package jetbrains.rsynk.io

enum class ChannelMessageTag(val code: Int) {
    Data(0),
    ErrorXfer(1),
    Info(2),
    Error(3),
    Warning(4),
    ErrorSocket(5),
    Log(6),
    Client(7),
    ErrorUtf(8),
    Redo(9),
    Flist(20),
    FlistEof(21),
    IOError(22),
    NOP(42),
    Done(86),
    Success(100),
    Deleted(101),
    NoSend(102)
}

private val msgTagOffset = 7
private val msgLengthMask = 0xFFFFFF

data class ChannelMessageHeader(val tag: ChannelMessageTag,
                                val length: Int)


object ChannelMessageHeaderDecoder {

    fun decodeHeader(tag: Int): ChannelMessageHeader? {
        val length = tag and msgLengthMask
        val tagObject = tagFromInt((tag shr 24) - msgTagOffset) ?: return null
        return ChannelMessageHeader(tagObject, length)
    }

    fun encodeHeader(header: ChannelMessageHeader): Int {
        return msgTagOffset + header.tag.code shl 24 or header.length
    }

    private fun tagFromInt(code: Int): ChannelMessageTag? {
        ChannelMessageTag.values().forEach {
            if (code == it.code) {
                return it
            }
        }
        return null
    }
}

