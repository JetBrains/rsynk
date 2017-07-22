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
import jetbrains.rsynk.files.TrackedFilesStorage
import jetbrains.rsynk.files.UnixDefaultFileSystemInfo
import jetbrains.rsynk.server.ExplicitCommandFactory
import jetbrains.rsynk.server.RsynkSshServer
import jetbrains.rsynk.server.SSHSessionFactory
import jetbrains.rsynk.settings.SshServerSettings
import org.apache.sshd.common.keyprovider.KeyPairProvider
import java.util.*

class Rsynk internal constructor(private val builder: RsynkBuilder) : AutoCloseable {

    companion object Builder {
        fun newBuilder() = RsynkBuilder.default
    }

    private val server: RsynkSshServer
    private val trackedFiles = ArrayList<RsynkFile>()
    private val filesProvider = object : TrackedFilesStorage {
        override fun getTrackedFiles(): List<RsynkFile> {
            return trackedFiles
        }

    }

    init {
        val sshSettings = sshSetting()
        val fileInfoReader = fileInfoReader()

        server = RsynkSshServer(
                sshSettings,
                ExplicitCommandFactory(sshSettings, fileInfoReader, filesProvider),
                SSHSessionFactory()
        )

        trackedFiles.addAll(builder.files)

        server.start()
    }

    fun addTrackingFiles(files: List<RsynkFile>): Rsynk {
        this.trackedFiles.addAll(files)
        return this
    }

    fun addTrackingFile(file: RsynkFile) {
        addTrackingFiles(listOf(file))
    }

    fun setTrackingFiles(files: List<RsynkFile>): Rsynk {
        this.trackedFiles.clear()
        addTrackingFiles(files)
        return this
    }

    override fun close() {
        server.stop()
    }

    private fun fileInfoReader(): FileInfoReader {
        return FileInfoReader(UnixDefaultFileSystemInfo())
    }

    private fun sshSetting() = object : SshServerSettings {
        override val port: Int = builder.port
        override val nioWorkers: Int = builder.nioWorkers
        override val commandWorkers: Int = builder.commandWorkers
        override val idleConnectionTimeout: Int = builder.idleConnectionTimeout
        override val maxAuthAttempts: Int = builder.maxAuthAttempts
        override val serverKeys: KeyPairProvider = builder.serverKeysProvider
        override val applicationNameNoSpaces: String = "rsynk"
    }
}


