package jetbrains.rsynk.command

import jetbrains.rsynk.data.VarintEncoder
import jetbrains.rsynk.exitvalues.InvalidFileException
import jetbrains.rsynk.exitvalues.NotSupportedException
import jetbrains.rsynk.exitvalues.ProtocolException
import jetbrains.rsynk.exitvalues.UnsupportedProtocolException
import jetbrains.rsynk.extensions.MAX_VALUE_UNSIGNED
import jetbrains.rsynk.files.*
import jetbrains.rsynk.flags.*
import jetbrains.rsynk.io.ReadingIO
import jetbrains.rsynk.io.WriteIO
import jetbrains.rsynk.options.Option
import jetbrains.rsynk.options.RequestOptions
import jetbrains.rsynk.protocol.RsynkServerStaticConfiguration
import mu.KLogging
import java.nio.ByteBuffer
import java.util.*


private data class PreviousFileSentFileInfoCache(val mode: Int?,
                                                 val user: User?,
                                                 val group: Group?,
                                                 val lastModified: Long?,
                                                 val path: String,
                                                 val sentUserNames: Set<User>,
                                                 val sendGroupNames: Set<Group>)

private val emptyPreviousFileCache = PreviousFileSentFileInfoCache(null, null, null, null, "", emptySet(), emptySet())

class RsyncServerSendCommand(private val fileInfoReader: FileInfoReader) : RsyncCommand {

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
                         output: WriteIO,
                         error: WriteIO) {
        exchangeProtocolVersions(input, output)
        writeCompatFlags(output)
        writeChecksumSeed(requestData.checksumSeed, output)

        val filter = receiveFilterList(input)
        sendFileList(requestData, filter, input, output)
    }

    /**
     * Writes server protocol version
     * and reads protocol client's version.
     *
     * @throws {@code UnsupportedProtocolException} if client's protocol version
     * either too old or too modern
     */
    private fun exchangeProtocolVersions(input: ReadingIO, output: WriteIO) {
        output.writeInt(RsynkServerStaticConfiguration.serverProtocolVersion)
        output.flush()
        val clientProtocolVersion = input.readInt()
        if (clientProtocolVersion < RsynkServerStaticConfiguration.clientProtocolVersionMin) {
            throw UnsupportedProtocolException("Client protocol version must be at least " +
                    RsynkServerStaticConfiguration.clientProtocolVersionMin)
        }
        if (clientProtocolVersion > RsynkServerStaticConfiguration.clientProtocolVersionMax) {
            throw UnsupportedProtocolException("Client protocol version must be no more than " +
                    RsynkServerStaticConfiguration.clientProtocolVersionMax)
        }
    }

    /**
     * Writes server's compat flags.
     */
    private fun writeCompatFlags(output: WriteIO) {
        val serverCompatFlags = RsynkServerStaticConfiguration.serverCompatFlags.encode()
        output.writeByte(serverCompatFlags)
        output.flush()
    }

    /**
     * Writes rolling checksum seed.
     * */
    private fun writeChecksumSeed(checksumSeed: Int, output: WriteIO) {
        output.writeInt(checksumSeed)
        output.flush()
    }


    /**
     * Receives filter list
     * */
    private fun receiveFilterList(input: ReadingIO): FilterList {

        var len = input.readInt()

        /* It's not clear why client writes those 4 bytes.
         * Rsync uses it's 'safe_read' int early communication stages
         * which deals with circular buffer. It's probably data remained
         * in buffer. Ignore it unless we figure out the byte is missing. */
        if (len > 1024 * 5) {
            len = input.readInt()
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

    private fun sendFileList(data: RequestData,
                             filterList: FilterList/* TODO: filter files (but not dot dir!) */,
                             reader: ReadingIO,
                             writer: WriteIO) {
        if (data.filePaths.size != 1) {
            throw NotSupportedException("Multiple files requests not implemented yet")
        }

        val paths = listOf(FileResolver.resolve(data.filePaths.single()))

        val fileList = FileListsBlocks(data.options.filesSelection is Option.FileSelection.Recurse)
        val initialBlock = fileList.addFileBlock(null, paths.map { path -> fileInfoReader.getFileInfo(path) })

        var prevFileCache = emptyPreviousFileCache
        initialBlock.files.forEach { _, file ->
            sendFileInfo(file, prevFileCache, data.options, writer)
            prevFileCache = PreviousFileSentFileInfoCache(file.mode,
                    file.user,
                    file.group,
                    file.lastModified,
                    file.path.toUri().path,
                    prevFileCache.sentUserNames + file.user,
                    prevFileCache.sendGroupNames + file.group)
        }
        writer.writeByte(0.toByte())

        if (data.options.preserveUser && !data.options.numericIds && data.options.filesSelection is Option.FileSelection.Recurse) {
            prevFileCache.sentUserNames.forEach { user ->
                sendUserId(user.uid, writer)
                sendUserName(user.name, writer)
            }
            writer.writeBytes(VarintEncoder.varlong(0, 1))
        }

        if (data.options.preserveGroup && !data.options.numericIds && data.options.filesSelection is Option.FileSelection.Recurse) {
            prevFileCache.sendGroupNames.forEach { group ->
                sendGroupId(group.gid, writer)
                sendGroupName(group.name, writer)
            }
            writer.writeBytes(VarintEncoder.varlong(0, 1))
        }
        writer.flush()

        if (initialBlock.files.isEmpty()) {
            if (data.options.filesSelection is Option.FileSelection.Recurse) {
                writer.writeByte((-1).toByte())
                writer.flush()
            }
            return
        }
        sendFiles(fileList, data, reader, writer)
    }

    private fun sendFileInfo(f: FileInfo, cache: PreviousFileSentFileInfoCache, options: RequestOptions, output: WriteIO) {
        var flags: Set<TransmitFlag> = HashSet()

        if (f.isDirectory) {
            flags += TransmitFlag.TopDirectory
        }

        if (f.isBlockDevice || f.isCharacterDevice || f.isSocket || f.isFIFO) {
            // TODO set or discard TransmitFlag.SameRdevMajor
        }

        if (f.mode == cache.mode) {
            flags += TransmitFlag.SameMode
        }

        if (options.preserveUser && f.user == cache.user) {
            flags += TransmitFlag.SameUserId
        } else if (!f.user.isRoot &&
                !options.numericIds &&
                options.filesSelection is Option.FileSelection.Recurse &&
                f.user in cache.sentUserNames) {
            flags += TransmitFlag.UserNameFollows
        }

        if (options.preserveGroup && f.group == cache.group) {
            flags += TransmitFlag.SameGroupId
        } else if (!f.group.isRoot &&
                !options.numericIds &&
                options.filesSelection is Option.FileSelection.Recurse &&
                f.group in cache.sendGroupNames) {
            flags += TransmitFlag.GroupNameFollows
        }

        if (f.lastModified == cache.lastModified) {
            flags += TransmitFlag.SameLastModifiedTime
        }

        val pathBytes = f.path.toUri().path.toByteArray()
        val commonPrefixLength = (pathBytes zip cache.path.toByteArray()).takeWhile { it.first == it.second }.size
        val suffix = Arrays.copyOfRange(pathBytes, commonPrefixLength, pathBytes.size)

        if (commonPrefixLength > 0) {
            flags += TransmitFlag.SameName
        }
        if (suffix.size > Byte.MAX_VALUE_UNSIGNED) {
            flags += TransmitFlag.SameLongName
        }
        if (flags.isEmpty() && !f.isDirectory) {
            flags += TransmitFlag.TopDirectory
        }

        val encodedFlags = flags.encode()
        if (flags.isEmpty() || encodedFlags and 0xFF00 != 0) {
            flags += TransmitFlag.ExtendedFlags
            output.writeChar(encodedFlags.toChar())
        } else {
            output.writeByte(encodedFlags.toByte())
        }

        if (TransmitFlag.SameName in flags) {
            output.writeByte(Math.min(commonPrefixLength, Byte.MAX_VALUE_UNSIGNED).toByte())
        }

        if (TransmitFlag.SameLongName in flags) {
            output.writeBytes(VarintEncoder.varlong(suffix.size.toLong(), 1))
        } else {
            output.writeByte(suffix.size.toByte())
        }
        output.writeBytes(ByteBuffer.wrap(suffix))

        output.writeBytes(VarintEncoder.varlong(f.size, 3))

        if (TransmitFlag.SameLastModifiedTime !in flags) {
            output.writeBytes(VarintEncoder.varlong(f.lastModified, 4))
        }

        if (TransmitFlag.SameMode !in flags) {
            output.writeInt(f.mode)
        }

        if (options.preserveUser && TransmitFlag.SameUserId !in flags) {
            sendUserId(f.user.uid, output)
            if (TransmitFlag.UserNameFollows in flags) {
                sendUserName(f.user.name, output)
            }
        }

        if (options.preserveGroup && TransmitFlag.SameGroupId !in flags) {
            sendGroupId(f.group.gid, output)
            if (TransmitFlag.GroupNameFollows in flags) {
                sendGroupName(f.group.name, output)
            }
        }

        if (options.preserveDevices || options.preserveSpecials) {
            //TODO send device info if this is a device or special
        } else if (options.preserveLinks) {
            //TODO send target if this is a symlink
        }
    }

    private fun sendUserId(uid: Int, writer: WriteIO) {
        writer.writeBytes(VarintEncoder.varlong(uid.toLong(), 1))
    }

    private fun sendUserName(name: String, writer: WriteIO) {
        val nameBytes = name.toByteArray()
        if (nameBytes.size > Byte.MAX_VALUE_UNSIGNED) {
            logger.warn { "Too long user name will be truncated to ${Byte.MAX_VALUE_UNSIGNED} bytes ($name)" }
        }
        writer.writeByte(nameBytes.size.toByte())
        writer.writeBytes(ByteBuffer.wrap(nameBytes))
    }

    private fun sendGroupId(gid: Int, writer: WriteIO) {
        writer.writeBytes(VarintEncoder.varlong(gid.toLong(), 1))
    }

    private fun sendGroupName(name: String, writer: WriteIO) {
        val nameBytes = name.toByteArray()
        if (nameBytes.size > Byte.MAX_VALUE_UNSIGNED) {
            logger.warn { "Too long group name will be truncated to ${Byte.MAX_VALUE_UNSIGNED} bytes ($name)" }
        }
        writer.writeByte(nameBytes.size.toByte())
        writer.writeBytes(ByteBuffer.wrap(nameBytes))
    }

    private fun sendFiles(fileListsBlocks: FileListsBlocks,
                          requestData: RequestData,
                          reader: ReadingIO,
                          writer: WriteIO) {

        var currentBlock: FileListBlock? = fileListsBlocks.peekBlock() ?: throw InvalidFileException("Sending empty file list blocks is not allowed")
        var filesInTransition = currentBlock?.files?.size ?: 0
        var eofSent = false

        val state = FileSendingState()

        while (state.current != FileSendingState.Phase.Stop) {
            if (fileListsBlocks.hasStubDirs &&
                    fileListsBlocks.blocksSize == 1 &&
                    filesInTransition < RsynkServerStaticConfiguration.fileListPartitionLimit / 2) {

                filesInTransition += expandAndSendStubDirs(fileListsBlocks,
                        RsynkServerStaticConfiguration.fileListPartitionLimit,
                        writer)
            }

            if (requestData.options.filesSelection is Option.FileSelection.Recurse
                    && !fileListsBlocks.hasStubDirs
                    && !eofSent) {

                writer.writeByte(encodeFileListIndex(FileListsCode.eof.code))
                eofSent = true
            }

            val index = decodeFileListIndex(reader.readBytes(1).first())

            when {
                index == FileListsCode.done.code -> {

                    if (requestData.options.filesSelection is Option.FileSelection.Recurse &&
                            fileListsBlocks.isNotEmpty()) {

                        val sentBlock = fileListsBlocks.popBlock()
                        filesInTransition -= sentBlock?.files?.size ?: 0

                        if (fileListsBlocks.blocksSize == 0) {
                            writer.writeByte(encodeFileListIndex(FileListsCode.done.code))
                        }
                    }

                    if (requestData.options.filesSelection !is Option.FileSelection.Recurse || fileListsBlocks.isEmpty()) {
                        state.nextState()
                        if (state.current != FileSendingState.Phase.Stop) {
                            writer.writeByte(encodeFileListIndex(FileListsCode.done.code))
                        }
                    }
                }

                index >= 0 -> {
                    val itemFlag = reader.readChar()
                    if (!ItemFlagsValidator.isFlagSupported(itemFlag.toInt())) {
                        throw NotSupportedException("Received not supported item flag ($itemFlag)")
                    }
                    val decodedItemFlags = itemFlag.decodeItemFlags()

                    if (ItemFlag.Transfer in decodedItemFlags) {
                        val block = currentBlock
                        if (block == null || block.files[index] == null) {
                            currentBlock = fileListsBlocks.peekBlock(index)
                        }

                        block ?: throw ProtocolException("Sent block index does not exist on server")

                        if (index != block.begin) {
                            block.markFileDeleted(index)
                            filesInTransition--
                        }
                        writer.writeByte(encodeFileListIndex(index))
                        writer.writeChar(itemFlag)
                    } else if (state.current == FileSendingState.Phase.Transfer) {
                        //line 1679
                    } else {
                        throw ProtocolException("Received index $index in unexpected transferring phase ${state.current.name}")
                    }
                }
                else -> {
                    throw ProtocolException("Invalid index $index")
                }
            }
        }
    }

    private fun expandAndSendStubDirs(fileListsBlocks: FileListsBlocks, filesLimit: Int, writeIO: WriteIO): Int {
        TODO()
    }

    private fun decodeFileListIndex(b: Byte): Int {
        TODO()
    }

    private fun encodeFileListIndex(index: Int): Byte {
        TODO()
    }
}


class FileSendingState {

    sealed class Phase(val name: String) {
        object Transfer : Phase("tranfer")
        object TearDownOne : Phase("tear-down-1")
        object TearDownTwo : Phase("tear-down-2")
        object Stop : Phase("stop")
    }

    var current: Phase = Phase.Transfer
        private set

    fun nextState() {
        when (current) {
            Phase.Transfer -> Phase.TearDownOne
            Phase.TearDownOne -> Phase.TearDownTwo
            Phase.TearDownTwo -> Phase.Stop
            Phase.Stop -> throw IllegalStateException("Cannot set next state after stop is set")
        }
    }

}
