package jetbrains.rsynk.command

import java.io.InputStream
import java.io.OutputStream


interface SSHCommand {
  fun execute(input: InputStream, output: OutputStream, error: OutputStream)
}