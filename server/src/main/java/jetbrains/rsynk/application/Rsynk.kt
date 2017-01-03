package jetbrains.rsynk.application

import jetbrains.rsynk.fs.Module
import jetbrains.rsynk.fs.Modules
import jetbrains.rsynk.server.*
import org.apache.sshd.common.keyprovider.KeyPairProvider
import java.io.File

class Rsynk(val port: Int,
            val nioWorkers: Int,
            val commandWorkers: Int,
            val idleConnectionTimeout: Int,
            val serverKeys: KeyPairProvider) {

  private val modules: Modules = Modules()
  private val server: SSHServer

  init {
    val settings = createSettings()
    server = SSHServer(settings, ExplicitCommandFactory(settings, modules), SSHSessionFactory())
  }

  private fun createSettings(): SSHSettings {
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

  fun start() = server.start()

  fun addModule(name: String, root: File) {
    modules.register(Module(name, root))
  }

  fun stop() = server.stop()
}
