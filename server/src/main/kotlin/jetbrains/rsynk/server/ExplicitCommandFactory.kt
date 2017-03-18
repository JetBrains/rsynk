package jetbrains.rsynk.server

import jetbrains.rsynk.command.AllCommandsHolder
import jetbrains.rsynk.command.CommandNotFoundException
import jetbrains.rsynk.exitvalues.RsyncExitCodes
import jetbrains.rsynk.exitvalues.RsynkException
import jetbrains.rsynk.io.SynchronousReadingIO
import jetbrains.rsynk.io.SynchronousWritingIO
import mu.KLogging
import org.apache.sshd.server.Command
import org.apache.sshd.server.CommandFactory
import org.apache.sshd.server.Environment
import org.apache.sshd.server.ExitCallback
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.Executors
import java.util.concurrent.Future

class ExplicitCommandFactory(settings: SSHSettings) : CommandFactory {

    companion object : KLogging()

    private val commands = AllCommandsHolder()
    private val optionsParser = SessionInfoParser()
    private val threadPool = Executors.newFixedThreadPool(settings.commandWorkers, threadFactory@ { runnable ->
        val thread = Thread(runnable, "ssh-command")
        thread.isDaemon = true
        return@threadFactory thread
    })

    override fun createCommand(cmd: String): Command {

        var exitCallback: ExitCallback? = null
        var runningCommand: Future<*>? = null

        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        var errorStream: OutputStream? = null


        return object : Command {
            override fun start(env: Environment) {
                val args = cmd.split(" ")

                if (args.isEmpty()) {
                    exit(RsyncExitCodes.ERROR_IN_RSYNC_PROTOCOL_DATA_STREAM, "No command received\n")
                }

                val command = try {
                    commands.resolve(args)
                } catch(e: CommandNotFoundException) {
                    exit(RsyncExitCodes.ERROR_IN_RSYNC_PROTOCOL_DATA_STREAM, "Unknown command: ${e.message}\n")
                    return
                }
                val stdin = inputStream
                if (stdin == null) {
                    exit(RsyncExitCodes.ERROR_IN_SOCKET_IO, "Input stream not set\n")
                    return
                }
                val stdout = outputStream
                if (stdout == null) {
                    exit(RsyncExitCodes.ERROR_IN_SOCKET_IO, "Output stream not set\n")
                    return
                }
                val stderr = errorStream
                if (stderr == null) {
                    exit(RsyncExitCodes.ERROR_IN_SOCKET_IO, "Error stream not set\n")
                    return
                }
                val sessionInfo = optionsParser.parse(args)
                runningCommand = threadPool.submit {
                    try {
                        command.execute(
                                sessionInfo,
                                SynchronousReadingIO(stdin),
                                SynchronousWritingIO(stdout),
                                SynchronousWritingIO(stderr)
                        )
                        exit(RsyncExitCodes.SUCCESS)
                    } catch (e: RsynkException) {
                        logger.info { "Command $args failed: with $e (${e.message})" }
                        writeError(e)
                        exit(e.exitCode)
                    } catch(t: Throwable) {
                        logger.error("Command $args failed: ${t.message}", t)
                        writeError(t)
                        exit(RsyncExitCodes.ERROR_IN_RSYNC_PROTOCOL_DATA_STREAM)
                    }
                }
            }

            private fun writeError(t: Throwable) {
                val message = t.message
                        if (message != null) {
                            errorStream?.apply {
                                write("$message\n".toByteArray())
                                flush()
                            }
                        }
            }

            override fun destroy() {
                try {
                    runningCommand?.cancel(true)
                } catch (t: Throwable) {
                    logger.error("cannot cancel running command: ${t.message}", t)
                }
            }

            override fun setExitCallback(callback: ExitCallback) {
                exitCallback = callback
            }

            override fun setInputStream(`in`: InputStream) {
                inputStream = `in`
            }

            override fun setErrorStream(err: OutputStream) {
                errorStream = err
            }

            override fun setOutputStream(out: OutputStream) {
                outputStream = out
            }

            fun exit(code: Int, message: String) = exitCallback?.onExit(code, message)

            fun exit(code: Int) = exit(code, "")
        }
    }
}
