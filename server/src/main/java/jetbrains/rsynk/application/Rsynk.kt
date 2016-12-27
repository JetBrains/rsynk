package jetbrains.rsynk.application

import jetbrains.rsynk.server.*

class Rsynk(val port: Int,
            val nioWorkers: Int,
            val commandWorkers: Int,
            val idleConnectionTimeout: Int,
            val serverKeyPath: String) {

  private val server: SSHServer

  init {
    val settings = createSettings()
    server = SSHServer(settings, SSHServerKeys(), ExplicitCommandFactory(settings), SSHSessionFactory())
  }

  private fun createSettings(): SSHSettings {
    val that: Rsynk = this
    return object : SSHSettings {
      override val port: Int = that.port
      override val nioWorkers: Int = that.nioWorkers
      override val commandWorkers: Int = that.commandWorkers
      override val idleConnectionTimeout: Int = that.idleConnectionTimeout
      override val maxAuthAttempts: Int = 3
      override val serverSSHKeyPath: String = that.serverKeyPath
      override val applicationNameNoSpaces: String = "Rsynk"
    }
  }

  fun start() = server.start()

  fun stop() = server.stop()
}

