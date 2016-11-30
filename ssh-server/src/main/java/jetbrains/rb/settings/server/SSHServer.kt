package jetbrains.rb.settings.server

import jetbrains.rb.settings.ApplicationSettings
import jetbrains.rb.settings.SSHSettings
import jetbrains.rb.settings.keys.ServerKeys
import org.apache.sshd.common.compression.BuiltinCompressions
import org.apache.sshd.common.io.nio2.Nio2ServiceFactoryFactory
import org.apache.sshd.server.SshServer
import org.apache.sshd.server.auth.password.PasswordAuthenticator
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator
import org.apache.sshd.server.forward.RejectAllForwardingFilter
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors


class SSHServer(private val sshSettings: SSHSettings,
                private val appSettings: ApplicationSettings,
                private val serverKeys: ServerKeys,
                private val shellCommand: ShellCommandFactory,
                private val explicitCommands: ExplicitCommandFactory,
                private val sessionFactory: SSHSessionFactory) {

  private val log = LoggerFactory.getLogger(javaClass)
  private val sshd = SshServer.setUpDefaultServer()

  private val nioChannelExecutor = Executors.newFixedThreadPool(sshSettings.nioWorkers, { r ->
    val thread = Thread("sshd-nio")
    thread.isDaemon = true
    thread
  })


  private fun configure() {
    sshd.port = sshSettings.port
    sshd.nioWorkers = sshSettings.nioWorkers
    sshd.properties.put(SshServer.SERVER_IDENTIFICATION, appSettings.applicationNameNoSpaces)
    sshd.properties.put(SshServer.MAX_AUTH_REQUESTS, sshSettings.maxAuthRequests.toString())
    sshd.properties.put(SshServer.IDLE_TIMEOUT, sshSettings.idleTimeout.toString())

    sshd.shellFactory = shellCommand.createShellCommand()
    sshd.commandFactory = explicitCommands
    sshd.sessionFactory = sessionFactory.createSessionFactory()
    sshd.addSessionListener(sessionFactory.createSessionListener())

    sshd.keyPairProvider = serverKeys.loadKeys()
    sshd.publickeyAuthenticator = PublickeyAuthenticator { username, publicKey, server -> true }
    sshd.passwordAuthenticator = PasswordAuthenticator { username, password, server -> false }

    sshd.ioServiceFactoryFactory = Nio2ServiceFactoryFactory(nioChannelExecutor, false)
    sshd.tcpipForwardingFilter = RejectAllForwardingFilter()
    sshd.compressionFactories = listOf(BuiltinCompressions.none)
  }

  fun start() {
    configure()
    log.info("Starting sshd server:" +
            "   port=${sshSettings.port}," +
            "   nio-workers=${sshSettings.nioWorkers}")
    sshd.start()
  }

  fun stop() {
    sshd.stop()
    log.info("SSH server stopped")
  }
}
