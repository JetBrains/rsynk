package jetbrains.rb.settings.server

import org.apache.sshd.common.Factory
import org.apache.sshd.server.Command

class ShellCommandFactory {
  fun createShellCommand(): Factory<Command> {
    throw UnsupportedOperationException("not implemented")
  }
}
