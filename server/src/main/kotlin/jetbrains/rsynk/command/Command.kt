package jetbrains.rsynk.command

import jetbrains.rsynk.io.ReadingIO
import jetbrains.rsynk.io.WritingIO
import jetbrains.rsynk.session.SessionInfo


interface Command {
  fun execute(sessionInfo: SessionInfo,
              input: ReadingIO,
              output: WritingIO,
              error: WritingIO)
}

interface RsyncCommand : Command {
  val matchArgs: List<String>
}
