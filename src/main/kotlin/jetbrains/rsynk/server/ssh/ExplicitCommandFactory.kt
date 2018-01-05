/**
 * Copyright 2016 - 2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jetbrains.rsynk.server.ssh

import jetbrains.rsynk.rsync.exitvalues.RsyncExitCodes
import jetbrains.rsynk.rsync.exitvalues.RsynkException
import jetbrains.rsynk.rsync.files.FileInfoReader
import jetbrains.rsynk.rsync.files.TrackedFilesProvider
import jetbrains.rsynk.server.application.WorkersThreadPool
import jetbrains.rsynk.server.command.CommandNotFoundException
import jetbrains.rsynk.server.command.CommandResolver
import mu.KLogging
import org.apache.sshd.server.Command
import org.apache.sshd.server.CommandFactory
import org.apache.sshd.server.Environment
import org.apache.sshd.server.ExitCallback
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

internal class ExplicitCommandFactory(settings: SSHSettings,
                                      fileInfoReader: FileInfoReader,
                                      trackedFiles: TrackedFilesProvider
) : CommandFactory {

    companion object : KLogging()

    private val commandsResolver = CommandResolver(fileInfoReader, trackedFiles)

    private val threadPool: ExecutorService

    init {
        val workersThreadPool = settings.workersThreadPool
        when (workersThreadPool) {
            is WorkersThreadPool.DefaultThreadPool -> {
                threadPool = Executors.newFixedThreadPool(workersThreadPool.workersNumber, threadFactory@ { runnable ->
                    val newThread = Thread(runnable, "ssh-command")
                    newThread.isDaemon = true
                    return@threadFactory newThread
                })
            }
            is WorkersThreadPool.CustomThreadPool -> {
                threadPool = workersThreadPool.executor
            }
        }
    }

    override fun createCommand(cmd: String): Command {

        var exitCallback: ExitCallback? = null
        var runningCommand: Future<*>? = null

        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        var errorStream: OutputStream? = null


        return object : Command {
            override fun start(env: Environment) {
                val args = cmd.split(" ")

                val command = try {
                    commandsResolver.resolve(args)
                } catch (e: CommandNotFoundException) {
                    exit(RsyncExitCodes.RsyncProtocolDataStreamError, "Unknown command: ${e.message}\n")
                    return
                }
                val stdin = inputStream
                if (stdin == null) {
                    exit(RsyncExitCodes.SocketIOError, "Input stream not set\n")
                    return
                }
                val stdout = outputStream
                if (stdout == null) {
                    exit(RsyncExitCodes.SocketIOError, "Output stream not set\n")
                    return
                }
                val stderr = errorStream
                if (stderr == null) {
                    exit(RsyncExitCodes.SocketIOError, "Error stream not set\n")
                    return
                }
                runningCommand = threadPool.submit {
                    try {
                        command.execute(
                                args,
                                stdin,
                                stdout
                        )

                        exit(RsyncExitCodes.Success)
                    } catch (e: RsynkException) {
                        logger.info { "Command $args failed: with $e (${e.message})" }
                        writeError(e)
                        exit(e.exitCode)
                    } catch (t: Throwable) {
                        logger.error(t, { "Command $args failed: ${t.message}" })
                        writeError(t)
                        exit(RsyncExitCodes.RsyncProtocolDataStreamError)
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
                    logger.error(t, { "Cannot cancel running command: ${t.message}" })
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
