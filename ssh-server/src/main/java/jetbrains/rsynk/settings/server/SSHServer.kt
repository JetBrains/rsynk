package jetbrains.rsynk.settings.server

import jetbrains.rsynk.settings.keys.ServerKeys
import org.apache.sshd.common.compression.BuiltinCompressions
import org.apache.sshd.common.io.nio2.Nio2ServiceFactoryFactory
import org.apache.sshd.server.SshServer
import org.apache.sshd.server.auth.password.PasswordAuthenticator
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator
import org.apache.sshd.server.forward.RejectAllForwardingFilter
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors


class SSHServer(private val settings: SSHSettings,
                private val serverKeys: ServerKeys,
                private val shellCommand: ShellCommandFactory,
                private val explicitCommands: ExplicitCommandFactory,
                private val sessionFactory: SSHSessionFactory) {

  private val log = LoggerFactory.getLogger(javaClass)
  private val sshd = SshServer.setUpDefaultServer()

  private val nioChannelExecutor = Executors.newFixedThreadPool(settings.nioWorkers, threadFactory@{ runnable ->
    val thread = Thread(runnable, "sshd-nio")
    thread.isDaemon = true
    return@threadFactory thread
  })


  private fun configure() {
    sshd.port = settings.port
    sshd.nioWorkers = settings.nioWorkers
    sshd.properties.put(SshServer.SERVER_IDENTIFICATION, settings.applicationNameNoSpaces)
    sshd.properties.put(SshServer.MAX_AUTH_REQUESTS, settings.maxAuthRequests.toString())
    sshd.properties.put(SshServer.IDLE_TIMEOUT, settings.idleTimeout.toString())

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
            "   port=${settings.port}," +
            "   nio-workers=${settings.nioWorkers}")
    sshd.start()
  }

  fun stop() {
    sshd.stop()
    log.info("SSH server stopped")
  }
}
