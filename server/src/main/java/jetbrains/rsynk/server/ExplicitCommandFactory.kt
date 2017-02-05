package jetbrains.rsynk.server

import jetbrains.rsynk.command.RsyncCommand
import jetbrains.rsynk.command.SSHCommand
import jetbrains.rsynk.exitvalues.RsynkException
import org.apache.sshd.server.Command
import org.apache.sshd.server.CommandFactory
import org.apache.sshd.server.Environment
import org.apache.sshd.server.ExitCallback
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Future

class ExplicitCommandFactory(settings: SSHSettings) : CommandFactory {

  private val log = LoggerFactory.getLogger(javaClass)
  private val commands = TreeMap<String, SSHCommand>()
  private val threadPool = Executors.newFixedThreadPool(settings.commandWorkers, threadFactory@ { runnable ->
    val thread = Thread(runnable, "ssh-command")
    thread.isDaemon = true
    return@threadFactory thread
  })

  init {
    commands["rsync"] = RsyncCommand()
  }

  override fun createCommand(_args: String): Command {
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
        log.error("exit callback for $_args is null")
      }
    }

    fun exit(code: Int) {
      exit(code, "")
    }

    return object : Command {
      override fun start(env: Environment) {
        val args = _args.split(" ")
        val resolvedCommand = commands[args[0]]
        if (resolvedCommand == null) {
          exit(127, "Cannot find ssh command: ${args[0]}")
          return
        }
        val stdin = input
        if (stdin == null) {
          exit(128, "Input stream not set")
          return
        }
        val stdout = output
        if (stdout == null) {
          exit(128, "Output stream not set")
          return
        }
        val stderr = error
        if (stderr == null) {
          exit(128, "Error stream not set")
          return
        }
        runningCommand = threadPool.submit {
          try {
            resolvedCommand.execute(args, stdin, stdout, stderr)
            exit(0)
          } catch (e: RsynkException) {
            log.debug("Command $args failed: ${e.message}", e)
            val message = e.message
            if (message != null) {
              error?.write(message.toByteArray())
            }
            exit(e.exitCode)
          } catch(t: Throwable) {
            log.error("Command $args failed: ${t.message}", t)
            val message = t.message
            if (message != null) {
              error?.write(message.toByteArray())
            }
            exit(1)
          }
        }
      }

      override fun destroy() {
        try {
          runningCommand?.cancel(true)
        } catch (t: Throwable) {
          log.error("cannot cancel running command: ${t.message}", t)
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
