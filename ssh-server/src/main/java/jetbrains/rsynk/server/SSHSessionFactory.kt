package jetbrains.rsynk.server

import org.apache.sshd.common.session.SessionListener
import org.apache.sshd.server.session.SessionFactory

class SSHSessionFactory {
  fun createSessionFactory(): SessionFactory {
    throw UnsupportedOperationException("not implemented")
  }

  fun createSessionListener(): SessionListener {
    throw UnsupportedOperationException("not implemented")
  }
}
