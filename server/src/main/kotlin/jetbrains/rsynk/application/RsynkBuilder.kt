package jetbrains.rsynk.application

import jetbrains.rsynk.files.RsynkFile
import org.apache.sshd.common.keyprovider.KeyPairProvider

@SuppressWarnings("unused")
class RsynkBuilder internal constructor(internal val port: Int,
                                        internal val nioWorkers: Int,
                                        internal val commandWorkers: Int,
                                        internal val idleConnectionTimeout: Int,
                                        internal val serverKeys: KeyPairProvider,
                                        internal val maxAuthAttempts: Int,
                                        internal val files: List<RsynkFile>) {
    companion object {
        internal val default = RsynkBuilder(
                port = 22,
                nioWorkers = 1,
                commandWorkers = 1,
                idleConnectionTimeout = 50 * 1000,
                serverKeys = KeyPairProvider { emptyList() },
                maxAuthAttempts = 2,
                files = emptyList())
    }

    fun setPort(port: Int): RsynkBuilder = new(port = port)

    fun setNioWorkersNumber(nioWorkers: Int) = new(nioWorkers = nioWorkers)

    fun setCommandWorkersNumber(commandWorkers: Int) = new(commandWorkers = commandWorkers)

    fun setIdleConnectionTimeout(timeoutInMilleseconds: Int) = new(idleConnectionTimeout = timeoutInMilleseconds)

    fun setServerKeysProvider(provider: KeyPairProvider) = new(serverKeys = provider)

    fun setMaxAuthAttempts(maxAuthAttempts: Int) = new(maxAuthAttempts = maxAuthAttempts)

    fun addFiles(files: List<RsynkFile>) = new(files = this.files + files)

    fun build() = Rsynk(this)

    private fun new(port: Int = this.port,
                    nioWorkers: Int = this.nioWorkers,
                    commandWorkers: Int = this.commandWorkers,
                    idleConnectionTimeout: Int = this.idleConnectionTimeout,
                    serverKeys: KeyPairProvider = this.serverKeys,
                    maxAuthAttempts: Int = this.maxAuthAttempts,
                    files: List<RsynkFile> = this.files) = RsynkBuilder(
            port,
            nioWorkers,
            commandWorkers,
            idleConnectionTimeout,
            serverKeys,
            maxAuthAttempts,
            files
    )
}
