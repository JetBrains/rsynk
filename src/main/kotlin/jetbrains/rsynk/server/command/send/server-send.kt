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
package jetbrains.rsynk.server.command.send

import jetbrains.rsynk.rsync.data.*
import jetbrains.rsynk.rsync.exitvalues.InvalidFileException
import jetbrains.rsynk.rsync.exitvalues.NotSupportedException
import jetbrains.rsynk.rsync.exitvalues.ProtocolException
import jetbrains.rsynk.rsync.exitvalues.UnsupportedProtocolException
import jetbrains.rsynk.rsync.extensions.MAX_VALUE_UNSIGNED
import jetbrains.rsynk.rsync.extensions.toLittleEndianBytes
import jetbrains.rsynk.rsync.files.*
import jetbrains.rsynk.rsync.flags.CompatFlags
import jetbrains.rsynk.rsync.flags.ItemFlag
import jetbrains.rsynk.rsync.flags.ItemFlagsValidator
import jetbrains.rsynk.rsync.flags.TransmitFlag
import jetbrains.rsynk.rsync.options.Option
import jetbrains.rsynk.rsync.options.RsyncRequestArguments
import jetbrains.rsynk.rsync.protocol.RsyncProtocolStaticConfig
import jetbrains.rsynk.server.command.Command
import jetbrains.rsynk.server.command.CommandArgumentsMatcher
import jetbrains.rsynk.server.command.CommandExecutionTimer
import jetbrains.rsynk.server.io.*
import mu.KLogging
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.file.Path
import java.util.*
import java.util.function.Consumer
import java.util.function.Supplier
import kotlin.experimental.and
import kotlin.experimental.or

internal class RsyncServerSendCommandResolver : CommandArgumentsMatcher {
    override fun matches(args: List<String>): Boolean {
        if (args.size < 4) {
            return false
        }
        if (args.any { it == "--daemon" || it == "daemon" }) {
            return false
        }
        return args[1] == "--server" && args[2] == "--sender"
    }
}

internal class RsyncServerSendCommand(private val fileInfoReader: FileInfoReader,
                                      private val trackedFiles: TrackedFilesProvider) : Command {

    companion object : KLogging()

    private val filesListIndexDecoder = FilesListIndexDecoder()
    private val filesListIndexEncoder = FilesListIndexEncoder()


    /**
     * Perform negotiation and send requested file.
     * The behaviour is identical to {@code $rsync --server --sender}
     * command execution
     *
     * Protocol phases enumerated and phases documented in protocol.md
     * */
    override fun execute(args: List<String>,
                         stdIn: InputStream,
                         stdOut: OutputStream) {

        val bytesCountingInputStream = BytesCountingInputStream(stdIn)
        val bytesCountingOutputStream = BytesCountingOutputStream(stdOut)

        val input = RsyncInput(bytesCountingInputStream)
        val output = RsyncBufferedOutput(bytesCountingOutputStream)
        val requestData = ServerSendRequestDataParser.parse(args)

        exchangeProtocolVersions(input, output)

        writeCompatFlags(requestData.arguments, output)
        writeChecksumSeed(requestData.checksumSeed, output)

        val rsyncInput = RsyncTaggingInput(stdIn)
        val rsyncOutput = RsyncTaggingOutput(stdOut)

        receiveFilterList(rsyncInput)

        val sendFilesResult = sendFilesList(requestData, rsyncOutput)

        if (sendFilesResult.filesList != null) {
            sendFiles(sendFilesResult.filesList, requestData, rsyncInput, rsyncOutput)
        }

        sendStats(bytesCountingInputStream.bytesRead,
                bytesCountingOutputStream.bytesWritten,
                sendFilesResult.filesList?.getTotalFilesSizeBytes() ?: 0L,
                sendFilesResult.buildFilesListTime,
                sendFilesResult.sendFilesListTime,
                rsyncOutput)

        finalGoodbye(rsyncInput, rsyncOutput)
    }


    private fun exchangeProtocolVersions(input: RsyncDataInput, output: RsyncDataOutput) {

        val clientProtocolVersion = input.readInt()

        output.writeInt(RsyncProtocolStaticConfig.serverProtocolVersion)
        output.flush()

        if (clientProtocolVersion < RsyncProtocolStaticConfig.clientProtocolVersionMin) {
            throw UnsupportedProtocolException("Client protocol version must be at least " +
                    RsyncProtocolStaticConfig.clientProtocolVersionMin)
        }
        if (clientProtocolVersion > RsyncProtocolStaticConfig.clientProtocolVersionMax) {
            throw UnsupportedProtocolException("Client protocol version must be no more than " +
                    RsyncProtocolStaticConfig.clientProtocolVersionMax)
        }
    }

    private fun writeCompatFlags(arguments: RsyncRequestArguments, output: RsyncDataOutput) {
        var flags = RsyncProtocolStaticConfig.serverCompatFlags
        if (arguments.checksumSeedOrderFix) {
            flags = flags or CompatFlags.FIXED_CHECKSUM_SEED.mask
        }
        output.writeByte(flags)
        output.flush()
    }


    private fun writeChecksumSeed(checksumSeed: Int, output: RsyncDataOutput) {
        output.writeInt(checksumSeed)
        output.flush()
    }


    private fun receiveFilterList(input: RsyncDataInput) {

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
    }

    private fun sendFilesList(data: ServerSendRequestData,
                              writer: RsyncDataOutput): SendFilesListResult {
        val buildFilesListTimer = CommandExecutionTimer.start()

        val files = FileResolver(fileInfoReader, trackedFiles).resolve(data.files)

        if (data.arguments.filesSelection is Option.FileSelection.Recurse) {
            throw NotSupportedException("Recursive mode is not yet supported")
        }

        val filesList = FilesListBlocks(false)
        val initialBlock = filesList.addFileBlock(null, files.map { it.info }) //TODO: implement file boundaries restriction!

        var prevFileCache = emptyPreviousFileCache
        initialBlock.files.forEach { _, file ->
            sendFileInfo(file, prevFileCache, data.arguments, writer)
            prevFileCache = PreviousFileSentFileInfoCache(file.mode,
                    file.user,
                    file.group,
                    file.lastModified,
                    file.path.fileName.toString(),
                    prevFileCache.sentUserNames + file.user,
                    prevFileCache.sendGroupNames + file.group)
        }

        val buildFilesListExecutionTime = buildFilesListTimer.getTimeFromStart()
        val sendFilesListExecutionTimer = CommandExecutionTimer.start()

        writer.writeByte(0.toByte())

        if (data.arguments.preserveUser && !data.arguments.numericIds && data.arguments.filesSelection is Option.FileSelection.Recurse) {
            prevFileCache.sentUserNames.forEach { user ->
                sendUserId(user.uid, writer)
                sendUserName(user.name, writer)
            }
            writer.writeBytes(VarintEncoder.varlong(0, 1))
        }

        if (data.arguments.preserveGroup && !data.arguments.numericIds && data.arguments.filesSelection is Option.FileSelection.Recurse) {
            prevFileCache.sendGroupNames.forEach { group ->
                sendGroupId(group.gid, writer)
                sendGroupName(group.name, writer)
            }
            writer.writeBytes(VarintEncoder.varlong(0, 1))
        }
        val sendFilesListExecutionTime = sendFilesListExecutionTimer.getTimeFromStart()

        if (initialBlock.files.isEmpty()) {
            if (data.arguments.filesSelection is Option.FileSelection.Recurse) {
                writer.writeByte((-1).toByte())
                writer.flush()
            }
            return SendFilesListResult(null, buildFilesListExecutionTime, sendFilesListExecutionTime)
        }
        return SendFilesListResult(filesList, buildFilesListExecutionTime, sendFilesListExecutionTime)
    }

    private fun finalGoodbye(reader: RsyncDataInput,
                             writer: RsyncDataOutput) {

        val index = readIndexAndAttributes(reader, writer)

        if (index == FilesListsIndex.done.code) {
            encodeAndSendFilesListIndex(FilesListsIndex.done.code, writer)
            writer.flush()
        }

        if (index != FilesListsIndex.done.code) {
            throw ProtocolException("At a final goodbye expected index ${FilesListsIndex.done.code}, got $index")
        }
    }

    private fun readIndexAndAttributes(reader: RsyncDataInput,
                                       writer: RsyncDataOutput): Int {
        var index = 0
        while (true) {
            index = decodeAndReadFilesListIndex(reader, writer)
            if (index >= 0) {
                break
            }

            if (index == FilesListsIndex.done.code) {
                return index
            }

            TODO("Implement stats processing 'rsync.c, 326'")
        }

        val iflag = reader.readChar()
        TODO("To implement rsync.c 372")
    }

    private fun sendFileInfo(f: FileInfo,
                             cache: PreviousFileSentFileInfoCache,
                             arguments: RsyncRequestArguments,
                             output: RsyncDataOutput) {
        var flags = 0

        if (f.isDirectory) {
            flags = flags or TransmitFlag.TOP_DIRECTORY.mask
        }

        if (f.isBlockDevice || f.isCharacterDevice || f.isSocket || f.isFIFO) {
            // TODO set or discard TransmitFlag.SameRdevMajor
            throw InvalidFileException("${f.path} is not a regular file, only regular files are supported")
        }

        if (f.mode == cache.mode) {
            flags = flags or TransmitFlag.SAME_MODE.mask
        }

        if (arguments.preserveUser && f.user != cache.user) {
            if (!f.user.isRoot &&
                    !arguments.numericIds &&
                    arguments.filesSelection is Option.FileSelection.Recurse &&
                    f.user in cache.sentUserNames) {
                flags = flags or TransmitFlag.USER_NAME_FOLLOWS.mask
            }
        } else {
            flags = flags or TransmitFlag.SAME_USER_ID.mask
        }

        if (arguments.preserveGroup && f.group != cache.group) {
            if (!f.group.isRoot &&
                    !arguments.numericIds &&
                    arguments.filesSelection is Option.FileSelection.Recurse &&
                    f.group in cache.sendGroupNames) {
                flags = flags or TransmitFlag.GROUP_NAME_FOLLOWS.mask
            }
        } else {
            flags = flags or TransmitFlag.SAME_GROUP_ID.mask
        }

        if (f.lastModified == cache.lastModified) {
            flags = flags or TransmitFlag.SAME_LAST_MODIFIED.mask
        }

        val fileNameBytes = f.path.fileName.toString().toByteArray()
        val commonPrefixLength = (fileNameBytes zip cache.fileNameBytes.toByteArray()).takeWhile { it.first == it.second }.size
        val suffix = Arrays.copyOfRange(fileNameBytes, commonPrefixLength, fileNameBytes.size)

        if (commonPrefixLength > 0) {
            flags = flags or TransmitFlag.SAME_NAME.mask
        }
        if (suffix.size > Byte.MAX_VALUE_UNSIGNED) {
            flags = flags or TransmitFlag.SAME_LONG_NAME.mask
        }

        if (flags == 0 && !f.isDirectory) {
            flags = flags or TransmitFlag.TOP_DIRECTORY.mask
        }

        if (flags == 0 || flags and 0xFF00 != 0) {
            flags = flags or TransmitFlag.EXTENDED_FLAGS.mask
            output.writeChar(flags.toChar())
        } else {
            output.writeByte(flags.toByte())
        }

        if (flags and TransmitFlag.SAME_NAME.mask != 0) {
            output.writeByte(commonPrefixLength.toByte())
        }

        if (flags and TransmitFlag.SAME_LONG_NAME.mask != 0) {
            output.writeBytes(VarintEncoder.varlong(suffix.size.toLong(), 1))
        } else {
            output.writeByte(suffix.size.toByte())
        }
        output.writeBytes(ByteBuffer.wrap(suffix))

        output.writeBytes(VarintEncoder.varlong(f.size, 3))

        if (flags and TransmitFlag.SAME_LAST_MODIFIED.mask == 0) {
            output.writeBytes(VarintEncoder.varlong(f.lastModified, 4))
        }

        //missed thing flist.c line 569

        if (flags and TransmitFlag.SAME_MODE.mask == 0) {
            output.writeInt(f.mode)
        }

        if (arguments.preserveUser && flags and TransmitFlag.SAME_USER_ID.mask == 0) {
            sendUserId(f.user.uid, output)
            if (flags and TransmitFlag.USER_NAME_FOLLOWS.mask != 0) {
                sendUserName(f.user.name, output)
            }
        }

        if (arguments.preserveGroup && flags and TransmitFlag.SAME_GROUP_ID.mask == 0) {
            sendGroupId(f.group.gid, output)
            if (flags and TransmitFlag.GROUP_NAME_FOLLOWS.mask != 0) {
                sendGroupName(f.group.name, output)
            }
        }

        if (arguments.preserveDevices || arguments.preserveSpecials) {
            //TODO send device info if this is a device or special
            throw InvalidFileException("${f.path} is not a regular file, only regular files are supported")
        } else if (arguments.preserveLinks) {
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

    private fun sendUserId(uid: Int, writer: RsyncDataOutput) {
        writer.writeBytes(VarintEncoder.varlong(uid.toLong(), 1))
    }

    private fun sendUserName(name: String, writer: RsyncDataOutput) {
        val nameBytes = name.toByteArray()
        if (nameBytes.size > Byte.MAX_VALUE_UNSIGNED) {
            logger.warn { "Too long user name will be truncated to ${Byte.MAX_VALUE_UNSIGNED} bytes ($name)" }
        }
        writer.writeByte(nameBytes.size.toByte())
        writer.writeBytes(ByteBuffer.wrap(nameBytes))
    }

    private fun sendGroupId(gid: Int, writer: RsyncDataOutput) {
        writer.writeBytes(VarintEncoder.varlong(gid.toLong(), 1))
    }

    private fun sendGroupName(name: String, writer: RsyncDataOutput) {
        val nameBytes = name.toByteArray()
        if (nameBytes.size > Byte.MAX_VALUE_UNSIGNED) {
            logger.warn { "Too long group name will be truncated to ${Byte.MAX_VALUE_UNSIGNED} bytes ($name)" }
        }
        writer.writeByte(nameBytes.size.toByte())
        writer.writeBytes(ByteBuffer.wrap(nameBytes))
    }

    private fun sendFiles(blocks: FilesListBlocks,
                          requestData: ServerSendRequestData,
                          reader: RsyncDataInput,
                          writer: RsyncDataOutput) {


        val state = TransferStateMachine()

        stateLoop@ while (state.value != TransferStateMachine.State.Stop) {
            val index = decodeAndReadFilesListIndex(reader, writer)
            val iflags = if (index == FilesListsIndex.done.code) {
                0
            } else {
                reader.readChar().toInt()
            }
            if (!ItemFlagsValidator.isFlagSupported(iflags)) {
                throw NotSupportedException("Received not supported item flag ($iflags)")
            }

            when {
                index == FilesListsIndex.done.code -> {
                    state.next()
                    if (state.value != TransferStateMachine.State.Stop) {
                        encodeAndSendFilesListIndex(FilesListsIndex.done.code, writer)
                    }
                    continue@stateLoop
                }

                index >= 0 -> {

                    if (CompatFlags.INC_RECURSE.mask and RsyncProtocolStaticConfig.serverCompatFlags != 0.toByte()) {
                        throw NotSupportedException("It's time to implement extra file list sending (sender.c line 234)")
                    }

                    val block = blocks.peekBlock(0) ?: blocks.peekBlock(index)
                            ?: throw ProtocolException("Invalid file index received: cannot find corresponding block (blocks=$blocks, index=$index)")
                    val file = block.files[index]
                            ?: throw ProtocolException("Invalid file index received: cannot find corresponding file (files=${block.files}, index=$index)")

                    if (iflags and ItemFlag.TRANSFER.mask == 0) {
                        filesListIndexEncoder.encodeAndSend(index, Consumer { b -> writer.writeByte(b) })
                        writer.writeBytes(VarintEncoder.varint(iflags))

                        // TODO: send stats

                        if (iflags and ItemFlag.BASIC_TYPE_FOLLOWS.mask != 0) {
                            throw NotSupportedException("It's time to support fnamecpm_type (sender.c line 177)")
                        }

                        if (iflags and ItemFlag.XNAME_FOLLOWS.mask != 0) {
                            throw NotSupportedException("It's time to support xname (sender.c line 179)")
                        }

                        if (requestData.arguments.preserveXattrs) {
                            throw NotSupportedException("It's time to support sending xattr request (sender.c line 183)")
                        }

                        if (iflags and ItemFlag.IS_NEW.mask != 0) {
                            // TODO: update statistic (sender.c line 273)
                        }
                        continue@stateLoop
                    }

                    val checksumHeader = receiveChecksumHeader(reader)
                    val checksum = receiveChecksum(checksumHeader, reader)
                    filesListIndexEncoder.encodeAndSend(index, Consumer { b -> writer.writeByte(b) })

                    writer.writeBytes(VarintEncoder.shortint(iflags))

                    sendChecksumHeader(checksumHeader, writer)

                    //TODO set compression

                    val blockSize = if (checksumHeader.isNewFile) FilesTransmission.defaultBlockSize else checksumHeader.blockLength
                    val bufferSizeMultiplier = if (checksumHeader.isNewFile) 1 else 10
                    FilesTransmission.runWithOpenedFile(file,
                            blockSize,
                            blockSize * bufferSizeMultiplier) { fileRepr ->

                        val calculatedChecksum = try {
                            if (checksumHeader.isNewFile) {
                                skipMatchesAndGetChecksum(fileRepr, file, writer)
                            } else {
                                sendMatchesAndGetChecksum(fileRepr, checksum, requestData.checksumSeed, writer)
                            }
                        } catch (t: Throwable) {
                            logger.error { "Failed to calculate checksum: ${t.message}, $t" }
                            byteArrayOf() //TODO
                        }

                        writer.writeBytes(ByteBuffer.wrap(calculatedChecksum))
                    }

                }
                else -> {
                    throw ProtocolException("Invalid index $index")
                }
            }
        }

        encodeAndSendFilesListIndex(FilesListsIndex.done.code, writer)
    }

    /* TODO: implement recursive sending
    private fun expandAndSendStubDirectories(filesListBlocks: FilesListBlocks,
                                             blockInTransmission: Int,
                                             sentFilesLimit: Int,
                                             requestData: ServerSendRequestData,
                                             writer: RsyncDataOutput): StubDirectoriesExpandingResult {

        var filesSent = 0
        var currentBlock = blockInTransmission

        while (filesListBlocks.hasStubDirs && filesSent < sentFilesLimit) {
            val stubDir = filesListBlocks.popStubDir(currentBlock) ?: throw ProtocolException("Invalid stub directory block index: $currentBlock")
            encodeAndSendFilesListIndex(FilesListsIndex.offset.code - currentBlock, writer)

            val expanded = expandStubDirectory(stubDir, requestData)
            val block = filesListBlocks.addFileBlock(stubDir, expanded)

            block.files.forEach { _index, file ->
                sendFileInfo(file, emptyPreviousFileCache, requestData.arguments, writer)
                filesSent++
            }
            sendBlockEnd(writer)
            currentBlock++
        }

        return StubDirectoriesExpandingResult(filesSent, currentBlock - blockInTransmission)
    }
    */

    /*
    private fun expandStubDirectory(directory: FileInfo,
                                    requestData: ServerSendRequestData): List<FileInfo> {
        val root = locateRootDirectoryPath(directory)

        val list = ArrayList<FileInfo>()
        Files.newDirectoryStream(directory.path).use {
            it.forEach { directoryEntry ->

                val relativePath = root.relativize(directoryEntry).normalize()
                val fileInfo = fileInfoReader.getFileInfo(directoryEntry)

                val element = when {
                    requestData.arguments.preserveLinks && fileInfo.isSymlink -> {
                        TODO()
                    }

                    requestData.arguments.preserveDevices && (fileInfo.isBlockDevice || fileInfo.isCharacterDevice) -> {
                        TODO()
                    }

                    requestData.arguments.preserveSpecials && (fileInfo.isFIFO || fileInfo.isSocket) -> {
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
    */


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

    private fun decodeAndReadFilesListIndex(reader: RsyncDataInput, writer: RsyncDataOutput): Int {
        writer.flush()
        val index = filesListIndexDecoder.readAndDecode(Supplier { reader.readBytes(1).first() })
        if (index == FilesListsIndex.done.code || index >= 0) {
            return index
        }
        throw UnsupportedOperationException("It's time to implement deletion statistic reading and sending (rsync.c line 326)")
    }

    private fun encodeAndSendFilesListIndex(index: Int, writer: RsyncDataOutput) {
        filesListIndexEncoder.encodeAndSend(index, Consumer { b -> writer.writeByte(b) })
    }

    private fun receiveChecksumHeader(reader: RsyncDataInput): ChecksumHeader {
        val chunkCount = reader.readInt()
        val blockLength = reader.readInt()
        val digestLength = reader.readInt()
        val remainder = reader.readInt()
        return ChecksumHeader(chunkCount, blockLength, digestLength, remainder)
    }

    private fun sendChecksumHeader(header: ChecksumHeader,
                                   writer: RsyncDataOutput) {
        writer.writeInt(header.chunkCount)
        writer.writeInt(header.blockLength)
        writer.writeInt(header.digestLength)
        writer.writeInt(header.remainder)
    }

    private fun receiveChecksum(header: ChecksumHeader,
                                reader: RsyncDataInput): Checksum {
        val checksum = Checksum(header)

        for (chunkIndex in 0 until header.chunkCount) {
            val rollingChecksum = RollingChecksumChunk(reader.readInt())
            val longChecksum = LongChecksumChunk(reader.readBytes(header.digestLength))
            checksum += ChecksumChunk(chunkIndex,
                    rollingChecksum,
                    longChecksum)
        }
        return checksum
    }

    private fun skipMatchesAndGetChecksum(fileRepresentation: FileInTransmission,
                                          fileInfo: FileInfo,
                                          writer: RsyncDataOutput): ByteArray {
        val md = LongChecksum.newMessageDigestInstance()
        var bytesSent = 0

        while (fileRepresentation.getWindowLength() > 0) {

            val bytes = fileRepresentation.array
            val offset = fileRepresentation.getStartOffset()
            val windowLength = fileRepresentation.getWindowLength()

            sendData(bytes,
                    offset,
                    windowLength,
                    writer)
            bytesSent += windowLength

            md.update(bytes, offset, windowLength)
            fileRepresentation.slide(windowLength)
        }

        writer.writeInt(0)

        if (bytesSent.toLong() != fileInfo.size) {
            logger.debug { "Sent $bytesSent bytes of ${fileInfo.size} file" }
        }

        return md.digest()
    }

    private fun sendMatchesAndGetChecksum(fileRepresentation: FileInTransmission,
                                          checksum: Checksum,
                                          checksumSeed: Int,
                                          writer: RsyncDataOutput): ByteArray {

        val fileChecksum = LongChecksum.newMessageDigestInstance()
        val chunkChecksum = LongChecksum.newMessageDigestInstance()

        fileRepresentation.setMarkOffsetRelativeToStart(0)

        val smallestChunk = if (checksum.header.remainder > 0) {
            checksum.header.remainder
        } else {
            checksum.header.blockLength
        }
        val matcher = ChecksumMatcher(checksum)

        var preferredIndex = 0
        var localChunkLongChecksum: ByteArray? = null
        var currentRollingChecksum = RollingChecksum.calculate(fileRepresentation.array,
                fileRepresentation.getStartOffset(),
                fileRepresentation.getWindowLength())

        while (fileRepresentation.getWindowLength() >= smallestChunk) {

            for (chunk in matcher.getMatches(currentRollingChecksum, fileRepresentation.getWindowLength(), preferredIndex)) {
                if (localChunkLongChecksum == null) {
                    chunkChecksum.update(fileRepresentation.array, fileRepresentation.getStartOffset(), fileRepresentation.getWindowLength())
                    chunkChecksum.update(checksumSeed.toLittleEndianBytes())
                    localChunkLongChecksum = Arrays.copyOf(chunkChecksum.digest(), chunk.longChecksumChunk.checksum.size)
                }

                if (Arrays.equals(localChunkLongChecksum, chunk.longChecksumChunk.checksum)) {
                    val bytesMarked = fileRepresentation.getMarkOffset()
                    sendData(fileRepresentation.array, fileRepresentation.getStartOffset(), bytesMarked, writer)

                    fileChecksum.update(fileRepresentation.array, fileRepresentation.getMarkOffset(), fileRepresentation.getTotalBytes())

                    preferredIndex = chunk.chunkIndex + 1
                    writer.writeInt(-1 * preferredIndex)

                    fileRepresentation.setMarkOffsetRelativeToStart(fileRepresentation.getWindowLength())
                    fileRepresentation.slide(fileRepresentation.getWindowLength() - 1)

                    currentRollingChecksum = RollingChecksum.calculate(fileRepresentation.array,
                            fileRepresentation.getStartOffset(),
                            fileRepresentation.getWindowLength())

                    localChunkLongChecksum = null
                    break
                }
            }

            currentRollingChecksum = RollingChecksum.rollBack(currentRollingChecksum,
                    fileRepresentation.getWindowLength(),
                    fileRepresentation.array[fileRepresentation.getStartOffset()])

            if (fileRepresentation.isFull()) {
                sendData(fileRepresentation.array, fileRepresentation.getFirstOffset(), fileRepresentation.getTotalBytes(), writer)

                fileChecksum.update(fileRepresentation.array,
                        fileRepresentation.getFirstOffset(),
                        fileRepresentation.getTotalBytes())

                fileRepresentation.setMarkOffsetRelativeToStart(fileRepresentation.getWindowLength())
                fileRepresentation.slide(fileRepresentation.getWindowLength())
            } else {
                fileRepresentation.slide(1)
            }

            if (fileRepresentation.getWindowLength() == checksum.header.blockLength) {
                currentRollingChecksum = RollingChecksum.rollForward(currentRollingChecksum,
                        fileRepresentation.array[fileRepresentation.getEndOffset()])
            }
        }

        sendData(fileRepresentation.array,
                fileRepresentation.getFirstOffset(),
                fileRepresentation.getTotalBytes(),
                writer)

        fileChecksum.update(fileRepresentation.array, fileRepresentation.getFirstOffset(), fileRepresentation.getTotalBytes())
        writer.writeInt(0)

        return fileChecksum.digest()
    }

    private fun sendStats(bytesRead: Long,
                          bytesWritten: Long,
                          totalFilesSizeBytes: Long,
                          filesListBuildTimeMS: Long,
                          filesListTransferTimeMS: Long,
                          writer: RsyncDataOutput) {
        writer.writeBytes(VarintEncoder.varlong(bytesRead, 3))
        writer.writeBytes(VarintEncoder.varlong(bytesWritten, 3))
        writer.writeBytes(VarintEncoder.varlong(totalFilesSizeBytes, 3))
        writer.writeBytes(VarintEncoder.varlong(filesListBuildTimeMS, 3))
        writer.writeBytes(VarintEncoder.varlong(filesListTransferTimeMS, 3))
    }

    private fun sendData(bytes: ByteArray,
                         offset: Int,
                         length: Int,
                         writer: RsyncDataOutput) {

        var currentOffset = offset
        val endOffset = offset + length - 1

        while (currentOffset <= endOffset) {
            val chunkLength = Math.min(RsyncProtocolStaticConfig.chunkSize, endOffset - currentOffset + 1)
            writer.writeInt(chunkLength)
            writer.writeBytes(ByteBuffer.wrap(bytes, currentOffset, chunkLength))
            currentOffset += chunkLength
        }
    }

    private fun sendBlockEnd(writer: RsyncDataOutput) {
        writer.writeByte(0)
    }

}


private class TransferStateMachine  {

    sealed class State {
        object Transfer : State()
        object TearDownOne : State()
        object TearDownTwo : State()
        object Stop : State()
    }

    private var currentState: State = State.Transfer

    val value: State
        get() = currentState

    fun next() {
        val newState = when (currentState) {
            State.Transfer -> State.TearDownOne
            State.TearDownOne -> State.TearDownTwo
            State.TearDownTwo -> State.Stop
            State.Stop -> throw IllegalStateException("State iterator exhausted (`Stop` was already set)")
        }
        currentState = newState
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

private val emptyPreviousFileCache = PreviousFileSentFileInfoCache(null,
        null,
        null,
        null,
        "",
        emptySet(),
        emptySet())

private data class SendFilesListResult(
        val filesList: FilesListBlocks?,
        val buildFilesListTime: Long,
        val sendFilesListTime: Long
)
