package jetbrains.rsynk.command

import jetbrains.rsynk.exitvalues.NotSupportedException
import jetbrains.rsynk.exitvalues.UnsupportedProtocolException
import jetbrains.rsynk.extensions.littleEndianToInt
import jetbrains.rsynk.extensions.toLittleEndianBytes
import jetbrains.rsynk.files.FileInfoReader
import jetbrains.rsynk.files.FileList
import jetbrains.rsynk.files.FileResolver
import jetbrains.rsynk.files.FilterList
import jetbrains.rsynk.flags.encode
import jetbrains.rsynk.io.ReadingIO
import jetbrains.rsynk.io.WritingIO
import jetbrains.rsynk.options.Option
import jetbrains.rsynk.protocol.RsyncServerStaticConfiguration
import mu.KLogging

class RsyncServerSendCommand(private val fileInfo: FileInfoReader) : RsyncCommand {

    companion object : KLogging()

    /**
     * Perform negotiation and send requested file.
     * The behaviour is identical to {@code $rsync --server --sender}
     * command execution
     *
     * Protocol phases enumerated and phases documented in protocol.md
     * */
    override fun execute(requestData: RequestData,
                         input: ReadingIO,
                         output: WritingIO,
                         error: WritingIO) {
        exchangeProtocolVersions(input, output)

        writeCompatFlags(output)

        writeChecksumSeed(requestData.checksumSeed, output)

        val filter = receiveFilterList(input)
        sendFileList(requestData, filter, output)
    }

    /**
     * Writes server protocol version
     * and reads protocol client's version.
     *
     * @throws {@code UnsupportedProtocolException} if client's protocol version
     * either too old or too modern
     */
    private fun exchangeProtocolVersions(input: ReadingIO, output: WritingIO) {
        output.writeBytes(RsyncServerStaticConfiguration.serverProtocolVersion.toLittleEndianBytes())
        val clientProtocolVersion = input.readBytes(4).littleEndianToInt()
        if (clientProtocolVersion < RsyncServerStaticConfiguration.clientProtocolVersionMin) {
            throw UnsupportedProtocolException("Client protocol version must be at least " +
                    RsyncServerStaticConfiguration.clientProtocolVersionMin)
        }
        if (clientProtocolVersion > RsyncServerStaticConfiguration.clientProtocolVersionMax) {
            throw UnsupportedProtocolException("Client protocol version must be no more than " +
                    RsyncServerStaticConfiguration.clientProtocolVersionMax)
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
            throw NotSupportedException("Filter list is not supported")
            /*
            //TODO: receive & parse filter list
            //http://blog.mudflatsoftware.com/blog/2012/10/31/tricks-with-rsync-filter-rules/
            val bytes = input.readBytes(len).littleEndianToInt()
            len = input.readBytes(4).littleEndianToInt()
            */
        }
        return FilterList()
    }

    private fun sendFileList(data: RequestData, filterList: FilterList, output: WritingIO) {
        if (data.files.size != 1) {
            throw NotSupportedException("Multiple files requests not implemented yet")
        }

        val filesToSend = listOf(FileResolver.resolve(data.files.single()))
        if (!filterList.include(filesToSend.first())) {
            return
        }

        val fileList = FileList(data.options.directoryMode is Option.FileSelection.TransferDirectoriesRecurse)
        fileList.addFileSegment(null, filesToSend.map { fileInfo.getFileInfo(it, data.options) })
    }

    private fun write_varint(value: Int, output: WritingIO) {
        write_var_number(value.toLittleEndianBytes(), 1, output)
    }

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
