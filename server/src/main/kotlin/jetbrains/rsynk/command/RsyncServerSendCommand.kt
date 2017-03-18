package jetbrains.rsynk.command

import jetbrains.rsynk.exitvalues.InvalidFileException
import jetbrains.rsynk.exitvalues.UnsupportedProtocolException
import jetbrains.rsynk.extensions.MAX_VALUE_UNSIGNED
import jetbrains.rsynk.extensions.littleEndianToInt
import jetbrains.rsynk.extensions.toLittleEndianBytes
import jetbrains.rsynk.extensions.twoLowestBytes
import jetbrains.rsynk.files.FilterList
import jetbrains.rsynk.flags.TransmitFlags
import jetbrains.rsynk.flags.encode
import jetbrains.rsynk.io.ReadingIO
import jetbrains.rsynk.io.WritingIO
import jetbrains.rsynk.protocol.RsyncServerStaticConfiguration
import jetbrains.rsynk.session.SessionInfo
import mu.KLogging
import java.io.File

class RsyncServerSendCommand : RsyncCommand {

    override val matchArgs: List<String> = listOf("rsync", "--server", "--sender")

    companion object : KLogging()

    /**
     * Perform negotiation and send requested file.
     * The behaviour is identical to {@code $rsync --server --sender}
     * command execution
     *
     * Protocol phases enumerated and phases documented in protocol.md
     * */
    override fun execute(sessionInfo: SessionInfo,
                         input: ReadingIO,
                         output: WritingIO,
                         error: WritingIO) {
        setupProtocol(input, output)

        writeCompatFlags(output)

        writeChecksumSeed(sessionInfo.checskumSeed, output)

        val filter = receiveFilterList(input)
        sendFileList(sessionInfo.files, filter, output)
    }

    /**
     * Writes server protocol version
     * and reads protocol client's version.
     *
     * @throws {@code UnsupportedProtocolException} if client's protocol version
     * either too old or too modern
     */
    private fun setupProtocol(input: ReadingIO, output: WritingIO) {
        output.writeBytes(RsyncServerStaticConfiguration.serverProtocolVersion.toLittleEndianBytes())
        val clientProtocolVersion = input.readBytes(4).littleEndianToInt()
        if (clientProtocolVersion < RsyncServerStaticConfiguration.clientProtocolVersionMin) {
            throw UnsupportedProtocolException("Client protocol version must be at least ${RsyncServerStaticConfiguration.clientProtocolVersionMin}")
        }
        if (clientProtocolVersion > RsyncServerStaticConfiguration.clientProtocolVersionMax) {
            throw UnsupportedProtocolException("Client protocol version must be no more than ${RsyncServerStaticConfiguration.clientProtocolVersionMax}")
        }
    }

    /**
     * Writes server's compat flags.
     */
    private fun writeCompatFlags(output: WritingIO) {
        val serverCompatFlags = RsyncServerStaticConfiguration.serverCompatFlags.encode()
        output.writeBytes(byteArrayOf(serverCompatFlags))
    }

    /**
     * Writes rolling checksum seed.
     * */
    private fun writeChecksumSeed(checksumSeed: Int, output: WritingIO) {
        output.writeBytes(checksumSeed.toLittleEndianBytes())
    }


    /**
     * Receives filter list
     * */
    private fun receiveFilterList(input: ReadingIO): FilterList {

        var len = input.readBytes(4).littleEndianToInt()

        /* It's not clear why client writes those 4 bytes.
         * Rsync uses it's 'safe_read' int early communication stages
         * which deals with circular buffer. It's probably data remained
         * in buffer. Ignore it unless we figure out the byte is missing. */
        if (len > 1024 * 5) {
            len = input.readBytes(4).littleEndianToInt()
        }
        while (len != 0) {
            //TODO: receive & parse filter list
            //http://blog.mudflatsoftware.com/blog/2012/10/31/tricks-with-rsync-filter-rules/
            val bytes = input.readBytes(len).littleEndianToInt()
            len = input.readBytes(4).littleEndianToInt()
        }
        return FilterList()
    }

    private fun sendFileList(requestedFiles: List<String>, filterList: FilterList, output: WritingIO) {
        if (requestedFiles.size != 1) {
            //TODO: ok while goal is to send single file
            //TODO: then try multiple files, directories and whole combinatorics
            throw InvalidFileException("Multiple files requests not implemented yet")
        }
        val fileToSend = resolveFile(requestedFiles.single())
        if (!filterList.include(fileToSend)) {
            // gracefully exit, work is done when work is *none*
            return
        }

        //TODO: set transmit flags!
        val flags = emptySet<TransmitFlags>()
        val encodedFlags = flags.encode()
        if (encodedFlags.and(0xFF00) != 0 /* means value doesn't fit one byte */ || encodedFlags == 0) {
            /* Rsync plays very dirty there. Comment from native rsync sources:
             * We must make sure we don't send a zero flag byte or the
             * other end will terminate the flist transfer.  Note that
             * the use of XMIT_TOP_DIR on a non-dir has no meaning, so
             * it's harmless way to add a bit to the first flag byte. */
            output.writeBytes(encodedFlags.or(TransmitFlags.TopDirectory.value).twoLowestBytes)
        } else {
            output.writeBytes(byteArrayOf(encodedFlags.toByte()))
        }

        //TODO:
        /* This is used for recursive directory sending
         * used at flist.c ~537. I failed to find any
         * any l1 variable assignment. Very likely things
         * won't work in recursive directory transmission
         * all because of this variable */
        val lastName = ""
        val fileName = fileToSend.name

        val l1 = fileToSend.name.commonPrefixWith(lastName).length
        if (l1 > 0) {
            output.writeBytes(byteArrayOf(l1.toByte()))
        }
        val nameToSend = fileName.substring(l1)
        val l2 = nameToSend.length
        if (l2 > Byte.MAX_VALUE_UNSIGNED) {
            write_varint(l2, output)
        } else {
            output.writeBytes(byteArrayOf(l2.toByte()))
        }
        output.writeBytes(nameToSend.toByteArray())
        write_varlong(fileToSend.length(), 3, output)
        write_varlong(fileToSend.lastModified(), 4, output)
        //TODO: wiremode
    }

    private fun resolveFile(path: String): File {
        //TODO: very naive
        return File(path)
    }

    //TODO: such code doesn't suit the project
    private fun write_varint(value: Int, output: WritingIO) {
        write_var_number(value.toLittleEndianBytes(), 1, output)
    }

    //TODO: such code doesn't suit the project
    private fun write_varlong(value: Long, minBytes: Int, output: WritingIO) {
        write_var_number(value.toLittleEndianBytes(), minBytes, output)
    }

    private fun write_var_number(_bytes: ByteArray, minBytes: Int, output: WritingIO) {
        var cnt = _bytes.size
        val bytes = _bytes + byteArrayOf(0)
        while (cnt > minBytes && bytes[cnt] == 0.toByte()) {
            cnt--
        }
        val bit = 1.shl(7 - cnt + minBytes)
        if (bytes[cnt] >= bit) {
            cnt++
            bytes[0] = (bit - 1).inv().toByte()
        } else if (cnt > 1) {
            bytes[0] = bytes[cnt].toInt().or((bit * 2 - 1).inv()).toByte()
        } else {
            bytes[0] = bytes[cnt]
        }
        output.writeBytes(bytes, 0, cnt)
    }
}
