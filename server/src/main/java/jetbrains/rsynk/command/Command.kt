package jetbrains.rsynk.command

import jetbrains.rsynk.session.SessionInfo
import java.io.InputStream
import java.io.OutputStream


interface Command {
  fun execute(sessionInfo: SessionInfo,
              dir: String,
              files: List<String>,
              input: InputStream,
              output: OutputStream,
              error: OutputStream)
}

interface RsyncCommand : Command {
  val matchArgs: List<String>
}
