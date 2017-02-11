package jetbrains.rsynk.command

import org.slf4j.LoggerFactory
import java.io.InputStream
import java.io.OutputStream

class RsyncServerSendCommand : RsyncCommand {

  private val log = LoggerFactory.getLogger(javaClass)

  override fun execute(args: List<String>, input: InputStream, output: OutputStream, error: OutputStream) {

  }
}
