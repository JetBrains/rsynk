package jetbrains.rsynk.server

import jetbrains.rsynk.command.CommandNotFoundException
import jetbrains.rsynk.command.RsyncCommandsHolder
import jetbrains.rsynk.exitvalues.RsyncExitCodes
import jetbrains.rsynk.exitvalues.RsynkException
import org.apache.sshd.server.Command
import org.apache.sshd.server.CommandFactory
import org.apache.sshd.server.Environment
import org.apache.sshd.server.ExitCallback
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.Executors
import java.util.concurrent.Future

class ExplicitCommandFactory(settings: SSHSettings) : CommandFactory {

  private val log = LoggerFactory.getLogger(javaClass)

  private val rsyncCommands = RsyncCommandsHolder()
  private val threadPool = Executors.newFixedThreadPool(settings.commandWorkers, threadFactory@ { runnable ->
    val thread = Thread(runnable, "ssh-command")
    thread.isDaemon = true
    return@threadFactory thread
  })

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
        if (args.isEmpty()) {
          exit(RsyncExitCodes.ERROR_IN_RSYNC_PROTOCOL_DATA_STREAM, "No command received")
        }
        val commandsHolder = when (args.first()) {
          "rsync" -> rsyncCommands
          else -> {
            exit(RsyncExitCodes.ERROR_IN_RSYNC_PROTOCOL_DATA_STREAM, "Unknown command: $args")
            return
          }
        }
        val resolvedCommand = try {
          commandsHolder.resolve(args)
        } catch(e: CommandNotFoundException) {
          exit(RsyncExitCodes.ERROR_IN_RSYNC_PROTOCOL_DATA_STREAM, "Unknown command: ${e.message}")
          return
        }
        val stdin = input
        if (stdin == null) {
          exit(RsyncExitCodes.ERROR_IN_SOCKET_IO, "Input stream not set")
          return
        }
        val stdout = output
        if (stdout == null) {
          exit(RsyncExitCodes.ERROR_IN_SOCKET_IO, "Output stream not set")
          return
        }
        val stderr = error
        if (stderr == null) {
          exit(RsyncExitCodes.ERROR_IN_SOCKET_IO, "Error stream not set")
          return
        }
        runningCommand = threadPool.submit {
          try {
            resolvedCommand.execute(args, stdin, stdout, stderr)
            exit(RsyncExitCodes.SUCCESS)
          } catch (e: RsynkException) {
            log.debug("Command $args failed: ${e.message}", e)
            val message = e.message
            if (message != null) {
              error?.let {
                it.write(message.toByteArray())
                it.flush()
              }
            }
            exit(e.exitCode)
          } catch(t: Throwable) {
            log.error("Command $args failed: ${t.message}", t)
            val message = t.message
            if (message != null) {
              error?.let {
                it.write(message.toByteArray())
                it.flush()
              }
            }
            exit(RsyncExitCodes.ERROR_IN_RSYNC_PROTOCOL_DATA_STREAM)
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
