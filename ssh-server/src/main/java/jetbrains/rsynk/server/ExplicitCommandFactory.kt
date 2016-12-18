package jetbrains.rsynk.server

import jetbrains.rsynk.command.RsyncCommand
import jetbrains.rsynk.command.SSHCommand
import org.apache.sshd.server.Command
import org.apache.sshd.server.CommandFactory
import org.apache.sshd.server.Environment
import org.apache.sshd.server.ExitCallback
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.io.OutputStream
import java.util.TreeMap
import java.util.concurrent.Executors

class ExplicitCommandFactory(settings: SSHSettings) : CommandFactory {

  private val LOG = LoggerFactory.getLogger(javaClass)
  private val commands = TreeMap<String, SSHCommand>()
  private val threadPool = Executors.newFixedThreadPool(settings.commandWorkers, threadFactory@ { runnable ->
      val thread = Thread(runnable, "ssh-command")
      thread.isDaemon = true
      return@threadFactory thread
    })


  init {
    commands["rsync"] = RsyncCommand()
  }

  override fun createCommand(command: String): Command {
    var exitCallback: ExitCallback? = null
    var input: InputStream? = null
    var output: OutputStream? = null
    var error: OutputStream? = null

    return object : Command {
      override fun destroy() {
        throw UnsupportedOperationException("not implemented")
      }

      override fun start(env: Environment?) {
        val resolvedCommand = commands[command]
        if (resolvedCommand == null) {
          exitCallback?.onExit(127, "Cannot find ssh command with name $command")
        }
        threadPool.submit {
          resolvedCommand.execute(input, output, error)
        }
      }

      override fun setExitCallback(callback: ExitCallback) {
        exitCallback = callback
      }

      override fun setInputStream(`in`: InputStream) {
        input = `in`
      }

      override fun setErrorStream(err: OutputStream) {
        error = err
      }

      override fun setOutputStream(out: OutputStream) {
        output = out
      }

    }
  }
}
