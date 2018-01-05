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
package jetbrains.rsynk.server.application

import jetbrains.rsynk.rsync.files.FileInfoReader
import jetbrains.rsynk.rsync.files.RsynkFile
import jetbrains.rsynk.rsync.files.UnixDefaultFileSystemInfo
import jetbrains.rsynk.server.ssh.ExplicitCommandFactory
import jetbrains.rsynk.server.ssh.RsynkSshServer
import jetbrains.rsynk.server.ssh.SSHSessionFactory
import jetbrains.rsynk.server.ssh.SSHSettings
import org.apache.sshd.common.keyprovider.KeyPairProvider

class Rsynk internal constructor(port: Int,
                                 nioWorkers: Int?,
                                 workersThreadPool: WorkersThreadPool,
                                 idleTimeoutConnection: Long?,
                                 serverKeysProvider: KeyPairProvider) : AutoCloseable {

    companion object {
        @JvmStatic
        val builder: RsynkBuilder_SetPort
            get() = Builder.default
    }

    private val server: RsynkSshServer
    private val fileManager: TrackedFilesManager

    init {
        val sshSettings = object : SSHSettings {
            override val port: Int = port
            override val nioWorkers: Int? = nioWorkers
            override val workersThreadPool: WorkersThreadPool = workersThreadPool
            override val idleConnectionTimeout: Long = idleTimeoutConnection ?: 30 * 1000
            override val serverKeys: KeyPairProvider = serverKeysProvider
            override val applicationNameNoSpaces: String = "rsynk"
        }

        fileManager = TrackedFilesManager()

        server = RsynkSshServer(
                sshSettings,
                ExplicitCommandFactory(sshSettings, fileInfoReader, fileManager),
                SSHSessionFactory()
        )

        server.start()
    }

    fun trackFile(file: RsynkFile): Rsynk {
        fileManager.add(listOf(file))
        return this
    }

    fun trackFiles(files: List<RsynkFile>): Rsynk {
        fileManager.add(files)
        return this
    }

    fun stopTrackingAllFiles() {
        fileManager.removeAll()
    }

    override fun close() {
        server.stop()
    }

    private val fileInfoReader: FileInfoReader
        get() {
            //TODO: issue#2
            return FileInfoReader(UnixDefaultFileSystemInfo())
        }
}

