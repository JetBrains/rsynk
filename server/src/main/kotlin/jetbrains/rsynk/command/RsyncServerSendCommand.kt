/**
 * Copyright 2016 - 2017 JetBrains s.r.o.
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
package jetbrains.rsynk.command

import jetbrains.rsynk.data.*
import jetbrains.rsynk.exitvalues.InvalidFileException
import jetbrains.rsynk.exitvalues.NotSupportedException
import jetbrains.rsynk.exitvalues.ProtocolException
import jetbrains.rsynk.exitvalues.UnsupportedProtocolException
import jetbrains.rsynk.extensions.MAX_VALUE_UNSIGNED
import jetbrains.rsynk.extensions.toLittleEndianBytes
import jetbrains.rsynk.files.*
import jetbrains.rsynk.flags.*
import jetbrains.rsynk.io.AutoFlushingWriter
import jetbrains.rsynk.io.ReadingIO
import jetbrains.rsynk.io.WriteIO
import jetbrains.rsynk.options.Option
import jetbrains.rsynk.options.RequestOptions
import jetbrains.rsynk.protocol.RsyncMessage
import jetbrains.rsynk.protocol.RsyncMessageInterpreter
import jetbrains.rsynk.protocol.RsynkServerStaticConfiguration
import mu.KLogging
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.function.Consumer
import java.util.function.Supplier


internal class RsyncServerSendCommand(private val fileInfoReader: FileInfoReader,
                                      private val trackingFiles: TrackingFilesProvider) : RsyncCommand {

    companion object : KLogging()

    private val fileListIndexDecoder = FileListIndexDecoder()
    private val fileListIndexEncoder = FileListIndexEncoder()
    private val messageInterpreter = RsyncMessageInterpreter()

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
        writeCompatFlags(requestData.options, output)
        writeChecksumSeed(requestData.checksumSeed, output)

        val message = input.readInt()
        messageInterpreter.decode(message).let {
            if (it !is RsyncMessage.Data) {
                throw ProtocolException("Expected Data message, received: $it")
            }
        }
        //TODO: make client messages encoding more (than this) abstract
        output.writeInt(117440534/*that's a message to client*/)

        val filter = receiveFilterList(input)
        sendFileList(requestData, filter, input, AutoFlushingWriter(output))
    }


    private fun exchangeProtocolVersions(input: ReadingIO, output: WriteIO) {

        val clientProtocolVersion = input.readInt()

        output.writeInt(RsynkServerStaticConfiguration.serverProtocolVersion)
        output.flush()

        if (clientProtocolVersion < RsynkServerStaticConfiguration.clientProtocolVersionMin) {
            throw UnsupportedProtocolException("Client protocol version must be at least " +
                    RsynkServerStaticConfiguration.clientProtocolVersionMin)
        }
        if (clientProtocolVersion > RsynkServerStaticConfiguration.clientProtocolVersionMax) {
            throw UnsupportedProtocolException("Client protocol version must be no more than " +
                    RsynkServerStaticConfiguration.clientProtocolVersionMax)
        }
    }

    private fun writeCompatFlags(options: RequestOptions, output: WriteIO) {
        val cf = HashSet<CompatFlag>()
        cf.addAll(RsynkServerStaticConfiguration.serverCompatFlags)
        // TODO: merge static and dynamic compat flags setup
        if (options.checksumSeedOrderFix) {
            cf += CompatFlag.FixChecksumSeed
        }
        val encoded = cf.encode()
        output.writeByte(encoded)
        output.flush()
    }


    private fun writeChecksumSeed(checksumSeed: Int, output: WriteIO) {
        output.writeInt(checksumSeed)
        output.flush()
    }


    private fun receiveFilterList(input: ReadingIO): FilterList {

        val len = input.readInt()

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
                             filterList: FilterList?/* TODO: filter files (but not dot dir!) */,
                             reader: ReadingIO,
                             writer: WriteIO) {

        val files = FileResolver(fileInfoReader, trackingFiles).resolve(data.filePaths)

        if (data.options.filesSelection is Option.FileSelection.Recurse) {
            throw NotSupportedException("Recursive mode is not yet supported")
        }

        val fileList = FileListsBlocks(false)
        val initialBlock = fileList.addFileBlock(null, files.map { it.info }) //TODO: implement file boundaries restriction!

        var prevFileCache = emptyPreviousFileCache
        initialBlock.files.forEach { _, file ->
            sendFileInfo(file, prevFileCache, data.options, writer)
            prevFileCache = PreviousFileSentFileInfoCache(file.mode,
                    file.user,
                    file.group,
                    file.lastModified,
                    file.path.fileName.toString(),
                    prevFileCache.sentUserNames + file.user,
                    prevFileCache.sendGroupNames + file.group)
        }

        //Successfully sent files metadata
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

        if (initialBlock.files.isEmpty()) {
            if (data.options.filesSelection is Option.FileSelection.Recurse) {
                writer.writeByte((-1).toByte())
                writer.flush()
            }
            return
        }

        val message = reader.readInt()
        messageInterpreter.decode(message).let {
            if (it !is RsyncMessage.Data) {
                throw ProtocolException("Expected Data message, received: $it")
            }
        }

        sendFiles(fileList, data, reader, writer)
    }

    private fun sendFileInfo(f: FileInfo,
                             cache: PreviousFileSentFileInfoCache,
                             options: RequestOptions,
                             output: WriteIO) {
        var flags: Set<TransmitFlag> = HashSet()

        if (f.isDirectory) {
            flags += TransmitFlag.TopDirectory
        }

        if (f.isBlockDevice || f.isCharacterDevice || f.isSocket || f.isFIFO) {
            // TODO set or discard TransmitFlag.SameRdevMajor
            throw InvalidFileException("${f.path} is not a regular file, only regular files are supported")
        }

        if (f.mode == cache.mode) {
            flags += TransmitFlag.SameMode
        }

        if (options.preserveUser && f.user != cache.user) {
            if (!f.user.isRoot &&
                    !options.numericIds &&
                    options.filesSelection is Option.FileSelection.Recurse &&
                    f.user in cache.sentUserNames) {
                flags += TransmitFlag.UserNameFollows
            }
        } else {
            flags += TransmitFlag.SameUserId
        }

        if (options.preserveGroup && f.group != cache.group) {
            if (!f.group.isRoot &&
                    !options.numericIds &&
                    options.filesSelection is Option.FileSelection.Recurse &&
                    f.group in cache.sendGroupNames) {
                flags += TransmitFlag.GroupNameFollows
            }
        } else {
            flags += TransmitFlag.SameGroupId
        }

        if (f.lastModified == cache.lastModified) {
            flags += TransmitFlag.SameLastModifiedTime
        }

        val fileNameBytes = f.path.fileName.toString().toByteArray()
        val commonPrefixLength = (fileNameBytes zip cache.fileNameBytes.toByteArray()).takeWhile { it.first == it.second }.size
        val suffix = Arrays.copyOfRange(fileNameBytes, commonPrefixLength, fileNameBytes.size)

        if (commonPrefixLength > 0) {
            flags += TransmitFlag.SameName
        }
        if (suffix.size > Byte.MAX_VALUE_UNSIGNED) {
            flags += TransmitFlag.SameLongName
        }

        val encodedFlags = flags.encode()
        if (encodedFlags == 0 && !f.isDirectory) {
            flags += TransmitFlag.TopDirectory
        }

        if (encodedFlags == 0 || encodedFlags and 0xFF00 != 0) {
            flags += TransmitFlag.ExtendedFlags
            output.writeChar(encodedFlags.toChar())
        } else {
            output.writeByte(encodedFlags.toByte())
        }

        if (TransmitFlag.SameName in flags) {
            output.writeByte(commonPrefixLength.toByte())
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

        //missed thing flist.c line 569

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
            throw InvalidFileException("${f.path} is not a regular file, only regular files are supported")
        } else if (options.preserveLinks) {
            //TODO send target if this is a symlink
            throw InvalidFileException("${f.path} is not a regular file, only regular files are supported")
        }

        /* TODO:
        if (alwaysChecksum) {
            send partly prepared checksum
                    flist.c 638
        }
        */
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

    private fun sendFiles(blocks: FileListsBlocks,
                          requestData: RequestData,
                          reader: ReadingIO,
                          writer: WriteIO) {

        var eofSent = false

        val state = FilesSendingState()

        stateLoop@ while (state.current != FilesSendingState.State.Stop) {

            val index = decodeAndReadFileListIndex(reader, writer)
            val iflags = if (index == FileListsCode.done.code) {
                emptySet() // don't read flags if index is done
            } else {
                reader.readChar().decodeItemFlags()
            }
            if (!ItemFlagsValidator.isFlagSupported(iflags)) {
                throw NotSupportedException("Received not supported item flag ($iflags)")
            }

            when {
                index == FileListsCode.done.code -> {
                    state.nextState()
                    encodeAndSendFileListIndex(FileListsCode.done.code, writer)
                    continue@stateLoop
                }

                index >= 0 -> {

                    if (RsynkServerStaticConfiguration.serverCompatFlags.contains(CompatFlag.IncRecurse)) {
                        throw NotSupportedException("It's time to implement extra file list sending (sender.c line 234)")
                    }

                    val file = /*if (index - block.begin >= 0) {
                        block.files[index - block.begin]
                    } else {
                        //blocks.peekBlock(block.parent.files(index))
                        throw UnsupportedOperationException("Store parent index in blocks")
                    }*/ // <---- correct code for future
                            blocks.popBlock()!!.files[-1]!!

                    if (ItemFlag.Transfer !in iflags) {
                        fileListIndexEncoder.encodeAndSend(index, Consumer<Byte>{ b -> writer.writeByte(b) })
                        writer.writeBytes(VarintEncoder.varint(iflags.encode()))
                        /*
                        * TODO: stats
                        * */
                        if (ItemFlag.BasicTypeFollows in iflags) {
                            throw NotSupportedException("It's time to support fnamecpm_type (sender.c line 177)")
                        }

                        if (ItemFlag.XNameFollows in iflags) {
                            throw NotSupportedException("It's time to support xname (sender.c line 179)")
                        }

                        if (requestData.options.preserveXattrs) {
                            throw NotSupportedException("It's time to support sending xattr request (sender.c line 183)")
                        }

                        if (ItemFlag.IsNew in iflags) {
                            // TODO: update statistic (sender.c line 273)
                        }
                       continue@stateLoop
                    }

                    val checksumHeader = receiveChecksumHeader(reader)
                    val checksum = receiveChecksum(checksumHeader, reader)
                    fileListIndexEncoder.encodeAndSend(index, Consumer<Byte>{ b -> writer.writeByte(b) })
                    writer.writeBytes(VarintEncoder.varint(iflags.encode()))

                    sendChecksumHeader(checksumHeader, writer)


                    val blockSize = if (checksumHeader.isNewFile) FilesTransmission.defaultBlockSize else checksumHeader.blockLength
                    val bufferSizeMultiplier = if (checksumHeader.isNewFile) 1 else 10
                    FilesTransmission().runWithOpenedFile(file.path,
                            file.size,
                            blockSize,
                            blockSize * bufferSizeMultiplier) { fileRepr ->

                        val calculatedChacksum = try {

                            if (checksumHeader.isNewFile) {
                                skipMatchesAndGetChecksum(fileRepr, file, writer)
                            } else {
                                sendMatchesAndGetChecksum(fileRepr, checksum, requestData.checksumSeed, writer)
                            }

                        } catch (t: Throwable) {
                            byteArrayOf() //TODO
                        }

                        writer.writeBytes(ByteBuffer.wrap(calculatedChacksum))
                    }

                }
                else -> {
                    throw ProtocolException("Invalid index $index")
                }
            }
        }
    }

    private fun expandAndSendStubDirectories(fileListsBlocks: FileListsBlocks,
                                             blockInTransmission: Int,
                                             sentFilesLimit: Int,
                                             requestData: RequestData,
                                             writer: WriteIO): StubDirectoriesExpandingResult {

        var filesSent = 0
        var currentBlock = blockInTransmission

        while (fileListsBlocks.hasStubDirs && filesSent < sentFilesLimit) {
            val stubDir = fileListsBlocks.popStubDir(currentBlock) ?: throw ProtocolException("Invalid stub directory block index: $currentBlock")
            encodeAndSendFileListIndex(FileListsCode.offset.code - currentBlock, writer)

            val expanded = expandStubDirectory(stubDir, requestData)
            val block = fileListsBlocks.addFileBlock(stubDir, expanded)

            block.files.forEach { _index, file ->
                sendFileInfo(file, emptyPreviousFileCache, requestData.options, writer)
                filesSent++
            }
            sendBlockEnd(writer)
            currentBlock++
        }

        return StubDirectoriesExpandingResult(filesSent, currentBlock - blockInTransmission)
    }

    private fun expandStubDirectory(directory: FileInfo,
                                    requestData: RequestData): List<FileInfo> {
        val root = locateRootDirectoryPath(directory)

        val list = ArrayList<FileInfo>()
        Files.newDirectoryStream(directory.path).use {
            it.forEach { directoryEntry ->

                val relativePath = root.relativize(directoryEntry).normalize()
                val fileInfo = fileInfoReader.getFileInfo(directoryEntry)

                val element = when {
                    requestData.options.preserveLinks && fileInfo.isSymlink -> {
                        TODO()
                    }

                    requestData.options.preserveDevices && (fileInfo.isBlockDevice || fileInfo.isCharacterDevice) -> {
                        TODO()
                    }

                    requestData.options.preserveSpecials && (fileInfo.isFIFO || fileInfo.isSocket) -> {
                        TODO()
                    }

                    else -> {
                        FileInfo(relativePath, fileInfo.mode, fileInfo.size, fileInfo.lastModified, fileInfo.user, fileInfo.group)
                    }
                }
                list.add(element)
            }
        }

        return list
    }


    //TODO: move to FileInfo
    //TODO: or to separate util
    private fun locateRootDirectoryPath(fileInfo: FileInfo): Path {
        val fs = fileInfo.path.fileSystem

        val fullPath = fileInfo.path
        val relativePath = fs.getPath(fileInfo.path.fileName.toString())
        if (!fullPath.endsWith(relativePath)) {
            throw IllegalArgumentException("$relativePath is not a subpath of $fullPath")
        }

        val result = fullPath.subpath(0, fullPath.nameCount - relativePath.nameCount)
        if (fullPath.isAbsolute) {
            return fullPath.root.resolve(result)
        }
        return result
    }

    private fun decodeAndReadFileListIndex(reader: ReadingIO, writeIO: WriteIO): Int {
        writeIO.flush()
        val index = fileListIndexDecoder.readAndDecode(Supplier { reader.readBytes(1)[0] })
        if (index == FileListsCode.done.code || index >= 0) {
            return index
        }
        throw UnsupportedOperationException("It's time to implement deletion statistic reading and sending (rsync.c line 326)")
    }

    private fun encodeAndSendFileListIndex(index: Int, writer: WriteIO) {
        fileListIndexEncoder.encodeAndSend(index, Consumer { b -> writer.writeByte(b) })
        writer.flush()
    }

    private fun sendFileIndexAndItemFlag(index: Int, itemFlag: Char, writer: WriteIO) {
        encodeAndSendFileListIndex(index, writer)
        writer.writeChar(itemFlag)
    }

    private fun receiveChecksumHeader(reader: ReadingIO): ChecksumHeader {
        val chunkCount = reader.readInt()
        val blockLength = reader.readInt()
        val digestLength = reader.readInt()
        val remainder = reader.readInt()
        return ChecksumHeader(chunkCount, blockLength, digestLength, remainder)
    }

    private fun sendChecksumHeader(header: ChecksumHeader,
                                   writer: WriteIO) {
        writer.writeInt(header.chunkCount)
        writer.writeInt(header.blockLength)
        writer.writeInt(header.digestLength)
        writer.writeInt(header.remainder)
    }

    private fun receiveChecksum(header: ChecksumHeader,
                                reader: ReadingIO): Checksum {
        val checksum = Checksum(header)

        for (chunkIndex in 0..header.chunkCount - 1) {
            val rollingChecksum = RollingChecksumChunk(reader.readInt())
            val longChecksum = LongChecksumChunk(reader.readBytes(header.digestLength))
            checksum += ChecksumChunk(chunkIndex,
                    rollingChecksum,
                    longChecksum)
        }
        return checksum
    }

    private fun skipMatchesAndGetChecksum(fileRepresentation: TransmissionFileRepresentation,
                                          fileInfo: FileInfo,
                                          writer: WriteIO): ByteArray {
        val md = LongChecksum.newMessageDigestInstance()
        var bytesSent = 0

        while (fileRepresentation.currentWindowLength > 0) {

            val bytes = fileRepresentation.bytes
            val offset = fileRepresentation.offset
            val windowLength = fileRepresentation.currentWindowLength

            sendData(bytes,
                    offset,
                    windowLength,
                    writer)
            bytesSent += windowLength

            md.update(bytes, offset, windowLength)
            fileRepresentation.slide(windowLength)
        }

        if (bytesSent.toLong() != fileInfo.size) {
            logger.debug { "Sent $bytesSent bytes of ${fileInfo.size} file" }
        }

        return md.digest()
    }

    private fun sendMatchesAndGetChecksum(fileRepresentation: TransmissionFileRepresentation,
                                          checksum: Checksum,
                                          checksumSeed: Int,
                                          writer: WriteIO): ByteArray {

        val fileChecksum = LongChecksum.newMessageDigestInstance()
        val chunkChecksum = LongChecksum.newMessageDigestInstance()

        fileRepresentation.setMarkOffsetRelativetlyToStart(0)

        val smallestChunk = checksum.header.remainder - checksum.header.blockLength
        val matcher = ChecksumMatcher(checksum)

        var preferredIndex = 0
        var currentLongChecksum: ByteArray? = null
        var currentRollingChecksum = RollingChecksum.calculate(fileRepresentation.bytes,
                fileRepresentation.offset,
                fileRepresentation.currentWindowLength)

        while (fileRepresentation.currentWindowLength >= smallestChunk) {

            val matches = matcher.getMatches(currentRollingChecksum, fileRepresentation.currentWindowLength, preferredIndex)

            findingMatchesLoop@ for (chunk in matches) {

                val currentLongChecksumValue = currentLongChecksum
                val currentLongChecksumNotNull = if (currentLongChecksumValue != null) {
                    currentLongChecksumValue
                } else {
                    chunkChecksum.update(fileRepresentation.bytes, fileRepresentation.offset, fileRepresentation.currentWindowLength)
                    chunkChecksum.update(checksumSeed.toLittleEndianBytes())
                    val new = Arrays.copyOf(chunkChecksum.digest(), chunk.longChecksumChunk.checksum.size)
                    currentLongChecksum = new
                    new
                }

                if (Arrays.equals(currentLongChecksumNotNull, chunk.longChecksumChunk.checksum)) {
                    val bytesMarked = fileRepresentation.markedBytesCount
                    sendData(fileRepresentation.bytes, fileRepresentation.offset, bytesMarked, writer)

                    fileChecksum.update(fileRepresentation.bytes, fileRepresentation.markOffset, fileRepresentation.totalBytes)

                    preferredIndex = chunk.chunkIndex + 1
                    writer.writeInt(-1 * preferredIndex)

                    fileRepresentation.setMarkOffsetRelativetlyToStart(fileRepresentation.currentWindowLength)
                    fileRepresentation.slide(fileRepresentation.currentWindowLength - 1)

                    currentRollingChecksum = RollingChecksum.calculate(fileRepresentation.bytes,
                            fileRepresentation.offset,
                            fileRepresentation.currentWindowLength)

                    currentLongChecksum = null
                    break@findingMatchesLoop
                }
            }

            RollingChecksum.rollBack(currentRollingChecksum,
                    fileRepresentation.currentWindowLength,
                    fileRepresentation.bytes[fileRepresentation.offset])

            if (fileRepresentation.totalBytes == fileRepresentation.bytes.size) {

                sendData(fileRepresentation.bytes, fileRepresentation.getSmallestOffset(), fileRepresentation.totalBytes, writer)

                fileChecksum.update(fileRepresentation.bytes,
                        fileRepresentation.getSmallestOffset(),
                        fileRepresentation.totalBytes)

                fileRepresentation.setMarkOffsetRelativetlyToStart(fileRepresentation.currentWindowLength)
                fileRepresentation.slide(fileRepresentation.currentWindowLength)
            } else {
                fileRepresentation.slide(1)
            }

            if (fileRepresentation.currentWindowLength == checksum.header.blockLength) {
                currentRollingChecksum = RollingChecksum.rollForward(currentRollingChecksum,
                        fileRepresentation.bytes[fileRepresentation.endOffset])
            }
        }

        sendData(fileRepresentation.bytes,
                fileRepresentation.getSmallestOffset(),
                fileRepresentation.totalBytes,
                writer)

        fileChecksum.update(fileRepresentation.bytes, fileRepresentation.getSmallestOffset(), fileRepresentation.totalBytes)
        writer.writeInt(0)

        return fileChecksum.digest()
    }

    private fun sendData(bytes: ByteArray,
                         offset: Int,
                         length: Int,
                         writer: WriteIO) {

        var currentOffset = offset
        val endOffset = offset + length - 1

        while (currentOffset <= endOffset) {
            val chunkLength = Math.min(RsynkServerStaticConfiguration.chunkSize, endOffset - currentOffset + 1)
            writer.writeInt(chunkLength)
            writer.writeBytes(ByteBuffer.wrap(bytes, currentOffset, chunkLength))
            currentOffset += chunkLength
        }
    }

    private fun sendBlockEnd(writer: WriteIO) {
        writer.writeByte(0)
    }

}


class FilesSendingState {

    sealed class State(val name: String) {
        object Transfer : State("transfer")
        object TearDownOne : State("tear-down-1")
        object TearDownTwo : State("tear-down-2")
        object Stop : State("stop")
    }

    var current: State = State.Transfer
        private set

    fun nextState() {
        val newState = when (current) {
            State.Transfer -> State.TearDownOne
            State.TearDownOne -> State.TearDownTwo
            State.TearDownTwo -> State.Stop
            State.Stop -> throw IllegalStateException("State iterator exhausted (`Stop` was already set)")
        }
        current = newState
    }
}


private data class PreviousFileSentFileInfoCache(val mode: Int?,
                                                 val user: User?,
                                                 val group: Group?,
                                                 val lastModified: Long?,
                                                 val fileNameBytes: String,
                                                 val sentUserNames: Set<User>,
                                                 val sendGroupNames: Set<Group>)

private data class StubDirectoriesExpandingResult(val filesSent: Int,
                                                  val blocksSent: Int)

private val emptyPreviousFileCache = PreviousFileSentFileInfoCache(null, null, null, null, "", emptySet(), emptySet())
