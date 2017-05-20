package jetbrains.rsynk.application

import jetbrains.rsynk.files.FileInfoReader
import jetbrains.rsynk.files.RsynkFile
import jetbrains.rsynk.files.TrackingFilesProvider
import jetbrains.rsynk.files.UnixDefaultFileSystemInfo
import jetbrains.rsynk.server.ExplicitCommandFactory
import jetbrains.rsynk.server.RsynkSshServer
import jetbrains.rsynk.server.SSHSessionFactory
import jetbrains.rsynk.server.SSHSettings
import org.apache.sshd.common.keyprovider.KeyPairProvider
import java.util.*

class Rsynk internal constructor(private val builder: RsynkBuilder) : AutoCloseable {

    companion object Builder {
        fun newBuilder() = RsynkBuilder.default
    }

    private val server: RsynkSshServer
    private val trackingFiles = ArrayList<RsynkFile>()
    private val filesProvider = TrackingFilesProvider { trackingFiles }

    init {
        val sshSettings = sshSetting()
        val fileInfoReader = fileInfoReader()

        server = RsynkSshServer(
                sshSettings,
                ExplicitCommandFactory(sshSettings, fileInfoReader, filesProvider),
                SSHSessionFactory()
                )

        trackingFiles.addAll(builder.files)

        server.start()
    }

    fun addTrackingFiles(files: List<RsynkFile>): Rsynk {
        this.trackingFiles.addAll(files)
        return this
    }

    fun addTrackingFile(file: RsynkFile) {
        addTrackingFiles(listOf(file))
    }

    fun setTrackingFiles(files: List<RsynkFile>): Rsynk {
        this.trackingFiles.clear()
        addTrackingFiles(files)
        return this
    }

    override fun close() {
        server.stop()
    }

    private fun fileInfoReader(): FileInfoReader {
        return FileInfoReader(UnixDefaultFileSystemInfo())
    }

    private fun sshSetting() = object : SSHSettings {
        override val port: Int = builder.port
        override val nioWorkers: Int = builder.nioWorkers
        override val commandWorkers: Int = builder.commandWorkers
        override val idleConnectionTimeout: Int = builder.idleConnectionTimeout
        override val maxAuthAttempts: Int = builder.maxAuthAttempts
        override val serverKeys: KeyPairProvider = builder.serverKeysProvider
        override val applicationNameNoSpaces: String = "rsynk"
    }
}


