/**
 * Copyright 2016 - 2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jetbrains.rsynk.server

import mu.KLogging
import org.apache.sshd.common.compression.BuiltinCompressions
import org.apache.sshd.common.io.nio2.Nio2ServiceFactoryFactory
import org.apache.sshd.server.SshServer
import org.apache.sshd.server.auth.password.PasswordAuthenticator
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator
import org.apache.sshd.server.forward.RejectAllForwardingFilter
import java.util.concurrent.Executors


internal class RsynkSshServer(private val sshServerSettings: SshServerSettings,
                              private val explicitCommands: ExplicitCommandFactory,
                              private val sessionFactory: SSHSessionFactory) {

    companion object : KLogging()

    private val sshd = SshServer.setUpDefaultServer()

    private val nioChannelExecutor = Executors.newFixedThreadPool(sshServerSettings.nioWorkers, threadFactory@ { runnable ->
        val thread = Thread(runnable, "sshd-nio")
        thread.isDaemon = true
        return@threadFactory thread
    })


    private fun configure() {
        sshd.port = sshServerSettings.port
        sshd.nioWorkers = sshServerSettings.nioWorkers
        sshd.properties.put(SshServer.SERVER_IDENTIFICATION, sshServerSettings.applicationNameNoSpaces)
        sshd.properties.put(SshServer.MAX_AUTH_REQUESTS, sshServerSettings.maxAuthAttempts.toString())
        sshd.properties.put(SshServer.IDLE_TIMEOUT, sshServerSettings.idleConnectionTimeout.toString())

        sshd.commandFactory = explicitCommands
        sshd.sessionFactory = sessionFactory.createSessionFactory(sshd)
        sshd.addSessionListener(sessionFactory.createSessionListener())

        sshd.keyPairProvider = sshServerSettings.serverKeys
        sshd.publickeyAuthenticator = PublickeyAuthenticator { username, publicKey, server -> true }
        sshd.passwordAuthenticator = PasswordAuthenticator { username, password, server -> true }

        sshd.ioServiceFactoryFactory = Nio2ServiceFactoryFactory(nioChannelExecutor, false)
        sshd.tcpipForwardingFilter = RejectAllForwardingFilter()
        sshd.compressionFactories = listOf(BuiltinCompressions.none)
    }

    fun start() {
        configure()
        logger.info("Starting sshd server:\n" +
                " port=${sshServerSettings.port},\n" +
                " nio-workers=${sshServerSettings.nioWorkers},\n" +
                " command-workers=${sshServerSettings.commandWorkers},\n" +
                " idle-connection-timeout=${sshServerSettings.idleConnectionTimeout},\n" +
                " max-auth-requests=${sshServerSettings.maxAuthAttempts}\n")
        sshd.start()
    }

    fun stop() {
        sshd.stop()
        logger.info("SSH server stopped")
    }
}
