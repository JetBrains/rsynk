package jetbrains.rsynk.server

import org.apache.sshd.common.compression.BuiltinCompressions
import org.apache.sshd.common.io.nio2.Nio2ServiceFactoryFactory
import org.apache.sshd.common.keyprovider.KeyPairProvider
import org.apache.sshd.server.SshServer
import org.apache.sshd.server.auth.password.PasswordAuthenticator
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator
import org.apache.sshd.server.forward.RejectAllForwardingFilter
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors


class SSHServer(private val settings: SSHSettings,
                private val explicitCommands: ExplicitCommandFactory,
                private val sessionFactory: SSHSessionFactory) {

  private val log = LoggerFactory.getLogger(javaClass)
  private val sshd = SshServer.setUpDefaultServer()

  private val nioChannelExecutor = Executors.newFixedThreadPool(settings.nioWorkers, threadFactory@ { runnable ->
    val thread = Thread(runnable, "sshd-nio")
    thread.isDaemon = true
    return@threadFactory thread
  })


  private fun configure() {
    sshd.port = settings.port
    sshd.nioWorkers = settings.nioWorkers
    sshd.properties.put(SshServer.SERVER_IDENTIFICATION, settings.applicationNameNoSpaces)
    sshd.properties.put(SshServer.MAX_AUTH_REQUESTS, settings.maxAuthAttempts.toString())
    sshd.properties.put(SshServer.IDLE_TIMEOUT, settings.idleConnectionTimeout.toString())

    sshd.commandFactory = explicitCommands
    sshd.sessionFactory = sessionFactory.createSessionFactory(sshd)
    sshd.addSessionListener(sessionFactory.createSessionListener())

    sshd.keyPairProvider = settings.serverKeys
    sshd.publickeyAuthenticator = PublickeyAuthenticator { username, publicKey, server -> true }
    sshd.passwordAuthenticator = PasswordAuthenticator { username, password, server -> false }

    sshd.ioServiceFactoryFactory = Nio2ServiceFactoryFactory(nioChannelExecutor, false)
    sshd.tcpipForwardingFilter = RejectAllForwardingFilter()
    sshd.compressionFactories = listOf(BuiltinCompressions.none)
  }

  fun start() {
    configure()
    log.info("Starting sshd server:\n" +
            "   port=${settings.port},\n" +
            "   nio-workers=${settings.nioWorkers},\n" +
            "   command-workers=${settings.commandWorkers},\n" +
            "   idle-connection-timeout=${settings.idleConnectionTimeout},\n" +
            "   max-auth-requests=${settings.maxAuthAttempts}\n")
    sshd.start()
  }

  fun stop() {
    sshd.stop()
    log.info("SSH server stopped")
  }
}
