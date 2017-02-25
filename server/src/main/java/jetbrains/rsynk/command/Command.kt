package jetbrains.rsynk.command

import java.io.InputStream
import java.io.OutputStream


interface Command {
  fun execute(args: List<String>, input: InputStream, output: OutputStream, error: OutputStream)
}

interface RsyncCommand : Command {
  val matchArgs: List<String>
}
