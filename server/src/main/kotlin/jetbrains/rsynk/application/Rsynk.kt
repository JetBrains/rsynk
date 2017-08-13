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
package jetbrains.rsynk.application

import jetbrains.rsynk.files.FileInfoReader
import jetbrains.rsynk.files.RsynkFile
import jetbrains.rsynk.files.TrackedFilesProvider
import jetbrains.rsynk.files.UnixDefaultFileSystemInfo
import jetbrains.rsynk.server.ExplicitCommandFactory
import jetbrains.rsynk.server.RsynkSshServer
import jetbrains.rsynk.server.SSHSessionFactory
import jetbrains.rsynk.server.SSHSettings
import org.apache.sshd.common.keyprovider.KeyPairProvider
import java.util.*

class Rsynk internal constructor(private val builder: RsynkBuilder) : AutoCloseable {

    companion object Builder {
        fun newBuilder() = RsynkBuilder.default
    }

    private val server: RsynkSshServer
    private val trackedFiles = LinkedHashSet<RsynkFile>()
    private val filesProvider = object : TrackedFilesProvider {
        override fun getTrackedFiles(): List<RsynkFile> = trackedFiles.toList()
    }

    init {
        val sshSettings = object : SSHSettings {
            override val port: Int = builder.port
            override val nioWorkers: Int = builder.nioWorkers
            override val commandWorkers: Int = builder.commandWorkers
            override val idleConnectionTimeout: Int = builder.idleConnectionTimeoutMills
            override val maxAuthAttempts: Int = builder.maxAuthAttempts
            override val serverKeys: KeyPairProvider = builder.serverKeysProvider
            override val applicationNameNoSpaces: String = "rsynk"
        }

        server = RsynkSshServer(
                sshSettings,
                ExplicitCommandFactory(sshSettings, fileInfoReader, filesProvider),
                SSHSessionFactory()
        )

        server.start()
    }

    fun trackFile(file: RsynkFile): Rsynk {
        return trackFiles(listOf(file))
    }

    fun trackFiles(files: List<RsynkFile>): Rsynk {
        synchronized(trackedFiles) {
            trackedFiles.addAll(files)
            return this
        }
    }

    fun stopTrackingFile(file: RsynkFile) {
        synchronized(trackedFiles) {
            if (!trackedFiles.remove(file)) {
                val boundaries = file.getBoundariesCallable()
                throw IllegalArgumentException("File (${file.file}, offset=${boundaries.offset}, length=${boundaries.length} is not tracked by rsynk")
            }
        }
    }

    fun stopTrackingAllFiles() {
        synchronized(trackedFiles) {
            this.trackedFiles.clear()
        }
    }

    override fun close() {
        server.stop()
    }

    private val fileInfoReader: FileInfoReader
        get() {
            return FileInfoReader(UnixDefaultFileSystemInfo())
        }
}

