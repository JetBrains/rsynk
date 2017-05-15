package jetbrains.rsynk.server

import mu.KLogging
import org.apache.sshd.common.compression.BuiltinCompressions
import org.apache.sshd.common.io.nio2.Nio2ServiceFactoryFactory
import org.apache.sshd.server.SshServer
import org.apache.sshd.server.auth.password.PasswordAuthenticator
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator
import org.apache.sshd.server.forward.RejectAllForwardingFilter
import java.util.concurrent.Executors


internal class RsynkSshServer(private val sshSettings: SSHSettings,
                              private val explicitCommands: ExplicitCommandFactory,
                              private val sessionFactory: SSHSessionFactory) {

    companion object : KLogging()

    private val sshd = SshServer.setUpDefaultServer()

    private val nioChannelExecutor = Executors.newFixedThreadPool(sshSettings.nioWorkers, threadFactory@ { runnable ->
        val thread = Thread(runnable, "sshd-nio")
        thread.isDaemon = true
        return@threadFactory thread
    })


    private fun configure() {
        sshd.port = sshSettings.port
        sshd.nioWorkers = sshSettings.nioWorkers
        sshd.properties.put(SshServer.SERVER_IDENTIFICATION, sshSettings.applicationNameNoSpaces)
        sshd.properties.put(SshServer.MAX_AUTH_REQUESTS, sshSettings.maxAuthAttempts.toString())
        sshd.properties.put(SshServer.IDLE_TIMEOUT, sshSettings.idleConnectionTimeout.toString())

        sshd.commandFactory = explicitCommands
        sshd.sessionFactory = sessionFactory.createSessionFactory(sshd)
        sshd.addSessionListener(sessionFactory.createSessionListener())

        sshd.keyPairProvider = sshSettings.serverKeys
        sshd.publickeyAuthenticator = PublickeyAuthenticator { username, publicKey, server -> true }
        sshd.passwordAuthenticator = PasswordAuthenticator { username, password, server -> true }

        sshd.ioServiceFactoryFactory = Nio2ServiceFactoryFactory(nioChannelExecutor, false)
        sshd.tcpipForwardingFilter = RejectAllForwardingFilter()
        sshd.compressionFactories = listOf(BuiltinCompressions.none)
    }

    fun start() {
        configure()
        logger.info("Starting sshd server:\n" +
                " port=${sshSettings.port},\n" +
                " nio-workers=${sshSettings.nioWorkers},\n" +
                " command-workers=${sshSettings.commandWorkers},\n" +
                " idle-connection-timeout=${sshSettings.idleConnectionTimeout},\n" +
                " max-auth-requests=${sshSettings.maxAuthAttempts}\n")
        sshd.start()
    }

    fun stop() {
        sshd.stop()
        logger.info("SSH server stopped")
    }
}
