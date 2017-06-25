/**
 * Copyright 2016 - 2017 JetBrains s.r.o.
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
package jetbrains.rsynk.server

import jetbrains.rsynk.command.AllCommandsResolver
import jetbrains.rsynk.command.CommandNotFoundException
import jetbrains.rsynk.exitvalues.RsyncExitCodes
import jetbrains.rsynk.exitvalues.RsynkException
import jetbrains.rsynk.files.FileInfoReader
import jetbrains.rsynk.files.TrackingFilesProvider
import jetbrains.rsynk.io.BasicReadingIO
import jetbrains.rsynk.io.BufferedWriter
import jetbrains.rsynk.io.TaggingBufferedWriter
import mu.KLogging
import org.apache.sshd.server.Command
import org.apache.sshd.server.CommandFactory
import org.apache.sshd.server.Environment
import org.apache.sshd.server.ExitCallback
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.Executors
import java.util.concurrent.Future

internal class ExplicitCommandFactory(settings: SSHSettings,
                                      fileInfoReader: FileInfoReader,
                                      trackingFiles: TrackingFilesProvider) : CommandFactory {

    companion object : KLogging()

    private val commands = AllCommandsResolver(fileInfoReader, trackingFiles)
    private val threadPool = Executors.newFixedThreadPool(settings.commandWorkers, threadFactory@ { runnable ->
        val newThread = Thread(runnable, "ssh-command")
        newThread.isDaemon = true
        return@threadFactory newThread
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
                    exit(RsyncExitCodes.RsyncProtocolDataStreamError, "No command received\n")
                }

                val (command, requestData) = try {
                    commands.resolve(args)
                } catch(e: CommandNotFoundException) {
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
                                requestData,
                                BasicReadingIO(stdin),
                                TaggingBufferedWriter(stdout, 8 * 1024),
                                BufferedWriter(stderr)
                        )
                        exit(RsyncExitCodes.Success)
                    } catch (e: RsynkException) {
                        logger.info { "Command $args failed: with $e (${e.message})" }
                        writeError(e)
                        exit(e.exitCode)
                    } catch(t: Throwable) {
                        logger.error("Command $args failed: ${t.message}", t)
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
