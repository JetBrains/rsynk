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
import java.util.concurrent.Future

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
    var runningCommand: Future<*>? = null

    fun exit(code: Int, message: String) {
      val callback = exitCallback
      if (callback != null) {
        callback.onExit(code, message)
      } else {
        LOG.error("exit callback for $command command is null")
      }
    }

    fun exit(code: Int) {
      exit(code, "")
    }

    return object : Command {
      override fun start(env: Environment) {
        val resolvedCommand = commands[command]
        if (resolvedCommand == null) {
          exit(127, "Cannot find ssh command: $command")
          return
        }
        val stdin = input
        if (stdin == null) {
          exit(128, "Command input stream not set")
          return
        }
        val stdout = output
        if (stdout == null) {
          exit(128, "Command output stream not set")
          return
        }
        val stderr = error
        if (stderr == null) {
          exit(128, "Command error stream not set")
          return
        }
        runningCommand = threadPool.submit {
          try {
            resolvedCommand.execute(stdin, stdout, stderr)
          } catch(t: Throwable) {
            LOG.error("executing ssh $command command failed: ${t.message}", t)
          } finally {
            exit(0)
          }
        }
      }

      override fun destroy() {
        try {
          runningCommand?.cancel(true)
        } catch (t: Throwable) {
          LOG.error("cannot cancel running command: ${t.message}", t)
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
