package jetbrains.rsynk.application

import jetbrains.rsynk.files.FileInfoReader
import jetbrains.rsynk.files.UnixDefaultFileSystemInfo
import jetbrains.rsynk.server.ExplicitCommandFactory
import jetbrains.rsynk.server.SSHServer
import jetbrains.rsynk.server.SSHSessionFactory
import jetbrains.rsynk.server.SSHSettings
import org.apache.sshd.common.keyprovider.KeyPairProvider

class Rsynk(val port: Int,
            val nioWorkers: Int,
            val commandWorkers: Int,
            val idleConnectionTimeout: Int,
            val serverKeys: KeyPairProvider) : AutoCloseable {

    private val server: SSHServer

    init {

        val sshSettings = sshSetting()
        val fileInfoReader = fileInfoReader()

        server = SSHServer(
                sshSettings,
                ExplicitCommandFactory(sshSettings, fileInfoReader),
                SSHSessionFactory()
        )
    }

    fun startServer() {
        server.start()
    }

    override fun close() {
        server.stop()
    }


    private fun fileInfoReader(): FileInfoReader {
        return FileInfoReader(UnixDefaultFileSystemInfo())
    }

    private fun sshSetting(): SSHSettings {
        val that: Rsynk = this
        return object : SSHSettings {
            override val port: Int = that.port
            override val nioWorkers: Int = that.nioWorkers
            override val commandWorkers: Int = that.commandWorkers
            override val idleConnectionTimeout: Int = that.idleConnectionTimeout
            override val maxAuthAttempts: Int = 3
            override val serverKeys: KeyPairProvider = that.serverKeys
            override val applicationNameNoSpaces: String = "Rsynk"
        }
    }
}

