package jetbrains.rsynk.server

import org.apache.sshd.common.session.Session
import org.apache.sshd.common.session.SessionListener
import org.apache.sshd.server.ServerFactoryManager
import org.apache.sshd.server.session.ServerSession
import org.apache.sshd.server.session.SessionFactory
import org.slf4j.LoggerFactory

class SSHSessionFactory {

  private val log = LoggerFactory.getLogger(javaClass)

  fun createSessionFactory(server: ServerFactoryManager) = SessionFactory(server)

  fun createSessionListener() = object : SessionListener {
    override fun sessionCreated(session: Session?) {
      super.sessionCreated(session)
      session ?: return
      val serverSession = session as ServerSession?
      log.info("SSH session created, client ip=${serverSession?.clientAddress}")
    }

    override fun sessionEvent(session: Session?, event: SessionListener.Event?) {
      super.sessionEvent(session, event)
      event ?: return
      val serverSession = session as ServerSession?
      log.debug("SSH session event=$event, client ip=${serverSession?.clientAddress}")
    }

    override fun sessionException(session: Session?, t: Throwable?) {
      super.sessionException(session, t)
      if (t != null) {
        val serverSession = session as ServerSession?
        log.error("SSH session exception: ${t.message}, client ip=${serverSession?.clientAddress}", t)
      }
    }

    override fun sessionClosed(session: Session?) {
      super.sessionClosed(session)
      val serverSession = session as ServerSession?
      log.info("SSH session closed, client ip=${serverSession?.clientAddress}")
    }
  }
}
