package jetbrains.rsynk.server

import org.apache.sshd.common.keyprovider.KeyPairProvider
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider
import java.io.File

class SSHServerKeys {
  fun loadKeys(settings: SSHSettings): KeyPairProvider {
    val key = SimpleGeneratorHostKeyProvider(File(settings.serverSSHKeyPath))
    return KeyPairProvider { key.loadKeys() }
  }
}
