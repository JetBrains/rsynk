package jetbrains.rsynk.application

import jetbrains.rsynk.files.RsynkFile
import org.apache.sshd.common.keyprovider.KeyPairProvider

class RsynkBuilder internal constructor(var port: Int,
                                        var nioWorkers: Int,
                                        var commandWorkers: Int,
                                        var idleConnectionTimeout: Int,
                                        var serverKeysProvider: KeyPairProvider,
                                        var maxAuthAttempts: Int,
                                        internal val files: List<RsynkFile>) {
    companion object {
        internal val default = RsynkBuilder(
                port = 22,
                nioWorkers = 1,
                commandWorkers = 1,
                idleConnectionTimeout = 50 * 1000,
                serverKeysProvider = KeyPairProvider { emptyList() },
                maxAuthAttempts = 2,
                files = emptyList())
    }

    fun build() = Rsynk(this)
}
