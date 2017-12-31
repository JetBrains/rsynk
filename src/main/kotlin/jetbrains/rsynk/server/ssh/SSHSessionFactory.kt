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
package jetbrains.rsynk.server.ssh

import mu.KLogging
import org.apache.sshd.common.session.Session
import org.apache.sshd.common.session.SessionListener
import org.apache.sshd.server.ServerFactoryManager
import org.apache.sshd.server.session.ServerSession
import org.apache.sshd.server.session.SessionFactory

internal class SSHSessionFactory {

    companion object : KLogging()

    fun createSessionFactory(server: ServerFactoryManager) = SessionFactory(server)

    fun createSessionListener() = object : SessionListener {
        override fun sessionCreated(session: Session?) {
            session ?: return
            val serverSession = session as ServerSession?
            logger.info { "SSH session created, client ip=${serverSession?.clientAddress}" }
        }

        override fun sessionEvent(session: Session?, event: SessionListener.Event?) {
            event ?: return
            val serverSession = session as ServerSession?
            logger.debug { "SSH session event=$event, client ip=${serverSession?.clientAddress}" }
        }

        override fun sessionException(session: Session?, t: Throwable?) {
            if (t != null) {
                val serverSession = session as ServerSession?
                logger.error("SSH session exception: ${t.message}, client ip=${serverSession?.clientAddress}", t)
            }
        }

        override fun sessionClosed(session: Session?) {
            val serverSession = session as ServerSession?
            logger.info { "SSH session closed, client ip=${serverSession?.clientAddress}" }
        }
    }
}
