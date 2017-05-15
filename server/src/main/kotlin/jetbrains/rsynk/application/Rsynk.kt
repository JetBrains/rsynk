package jetbrains.rsynk.application

import jetbrains.rsynk.files.FileInfoReader
import jetbrains.rsynk.files.UnixDefaultFileSystemInfo
import jetbrains.rsynk.server.ExplicitCommandFactory
import jetbrains.rsynk.server.SSHServer
import jetbrains.rsynk.server.SSHSessionFactory
import jetbrains.rsynk.server.SSHSettings
import org.apache.sshd.common.keyprovider.KeyPairProvider

class Rsynk internal constructor(private val builder: RsynkBuilder) : AutoCloseable {

    companion object Builder {
        fun newBuilder() = RsynkBuilder.default
    }

    private val server: SSHServer
    private val files = ArrayList<RsynkFile>()

    init {
        val sshSettings = sshSetting()
        val fileInfoReader = fileInfoReader()

        server = SSHServer(
                sshSettings,
                ExplicitCommandFactory(sshSettings, fileInfoReader),
                SSHSessionFactory()
        )

        server.start()
    }

    fun addFiles(files: List<RsynkFile>) {
        this.files.addAll(files)
    }

    fun setFiles(files: List<RsynkFile>) {
        this.files.clear()
        addFiles(files)
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
        override val serverKeys: KeyPairProvider = builder.serverKeys
        override val applicationNameNoSpaces: String = "rsynk"
    }
}


