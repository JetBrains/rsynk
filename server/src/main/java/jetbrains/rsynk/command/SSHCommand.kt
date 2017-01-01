package jetbrains.rsynk.command

import java.io.InputStream
import java.io.OutputStream


interface SSHCommand {
  fun execute(args: List<String>, input: InputStream, output: OutputStream, error: OutputStream)
}