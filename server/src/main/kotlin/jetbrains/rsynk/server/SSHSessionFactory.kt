package jetbrains.rsynk.server

import mu.KLogging
import org.apache.sshd.common.session.Session
import org.apache.sshd.common.session.SessionListener
import org.apache.sshd.server.ServerFactoryManager
import org.apache.sshd.server.session.ServerSession
import org.apache.sshd.server.session.SessionFactory

class SSHSessionFactory {

  companion object: KLogging()

  fun createSessionFactory(server: ServerFactoryManager) = SessionFactory(server)

  fun createSessionListener() = object : SessionListener {
    override fun sessionCreated(session: Session?) {
      super.sessionCreated(session)
      session ?: return
      val serverSession = session as ServerSession?
      logger.info{"SSH session created, client ip=${serverSession?.clientAddress}"}
    }

    override fun sessionEvent(session: Session?, event: SessionListener.Event?) {
      super.sessionEvent(session, event)
      event ?: return
      val serverSession = session as ServerSession?
      logger.debug{"SSH session event=$event, client ip=${serverSession?.clientAddress}"}
    }

    override fun sessionException(session: Session?, t: Throwable?) {
      super.sessionException(session, t)
      if (t != null) {
        val serverSession = session as ServerSession?
        logger.error("SSH session exception: ${t.message}, client ip=${serverSession?.clientAddress}", t)
      }
    }

    override fun sessionClosed(session: Session?) {
      super.sessionClosed(session)
      val serverSession = session as ServerSession?
      logger.info{"SSH session closed, client ip=${serverSession?.clientAddress}"}
    }
  }
}
