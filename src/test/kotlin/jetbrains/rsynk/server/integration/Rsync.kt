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
package jetbrains.rsynk.server.integration

import jetbrains.rsynk.server.readFully
import org.junit.Assert
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.util.concurrent.TimeUnit

private interface RsyncProcessPath {
    val path: String

    fun isInstalled(): Boolean {
        println("Trying to execute rsync:'$path'")
        val pb = ProcessBuilder(path, "--version")
                .directory(Files.createTempDirectory("rsync_dir").toFile())
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectInput(ProcessBuilder.Redirect.PIPE)
        try {
            val p = pb.start()
            p.waitFor(5, TimeUnit.SECONDS)
            val bos = ByteArrayOutputStream(1024 * 5)
            readFully(p.inputStream, bos)
            val output = String(bos.toByteArray())
            if (output.contains("protocol version") && p.exitValue() == 0) {
                return true
            } else {
                println("Unrecognized $path --version command output: $output, exit code=${p.exitValue()}")
            }
        } catch (t: Throwable) {
            println("Exception caught during $path --version process invocation: ${t.message}")
        }
        return false
    }
}


/**
 * Runs rsync client from PATH environment
 */
internal class RsyncInPATH : RsyncProcessPath {
    override val path: String
        get () = "rsync"
}

/**
 * Runs macOS default rsync client
 */
internal class RsyncInPATH_macOS : RsyncProcessPath {
    override val path: String
        get () = "/usr/local/bin/rsync"
}

object Rsync {
    private val rsync: RsyncProcessPath = if (System.getProperty("os.name")?.toLowerCase()?.contains("mac") == true) {
        RsyncInPATH()
    } else {
        RsyncInPATH_macOS()
    }

    fun execute(from: String, to: String, port: Int, timeoutSec: Long, _params: String, ignoreErrors: Boolean = false): String {
        val params = if (_params.isEmpty()) "" else "-$_params"
        val args = listOf(rsync.path, params, "--protocol", "31", "-e", "ssh -p $port -o StrictHostKeyChecking=no", from, to)
        val pb = ProcessBuilder(args)
                .directory(Files.createTempDirectory("rsync_dir").toFile())
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectInput(ProcessBuilder.Redirect.PIPE)

        val process = pb.start()
        process.outputStream.close()
        try {
            process.waitFor(timeoutSec, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            throw Error("Rsync process is running longer than $timeoutSec sec, aborting...", e)
        }
        if (!ignoreErrors) {
            Assert.assertEquals("Rsync exit code not equals to 0\n" +
                    "args=$args\n" +
                    "stdout=${String(process.inputStream.readBytes())}\n" +
                    "stderr=${String(process.errorStream.readBytes())}",
                    0, process.exitValue())
        }
        val builder = StringBuilder()
        val stdout = String(process.inputStream.readBytes())
        val stderr = String(process.errorStream.readBytes())

        builder.append("Process stdout:\n")
        builder.append(stdout)
        builder.append("\n\n")
        builder.append("Process stderr:\n")
        builder.append(stderr)
        return builder.toString()
    }

    fun isInstalled(): Boolean = rsync.isInstalled()
}
