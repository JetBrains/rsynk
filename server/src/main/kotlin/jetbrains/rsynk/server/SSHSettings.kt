package jetbrains.rsynk.server

import org.apache.sshd.common.keyprovider.KeyPairProvider

interface SSHSettings {
    val port: Int
    val nioWorkers: Int
    val commandWorkers: Int
    val idleConnectionTimeout: Int
    val maxAuthAttempts: Int
    val applicationNameNoSpaces: String
    val serverKeys: KeyPairProvider
}
