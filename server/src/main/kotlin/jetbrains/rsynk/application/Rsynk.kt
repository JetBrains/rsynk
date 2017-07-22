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

import jetbrains.rsynk.files.RsynkFile
import jetbrains.rsynk.files.TrackedFilesStorage
import jetbrains.rsynk.files.mkdirs
import jetbrains.rsynk.server.ExplicitCommandFactory
import jetbrains.rsynk.server.RsynkSshServer
import jetbrains.rsynk.server.SSHSessionFactory
import jetbrains.rsynk.settings.RsyncSettings
import jetbrains.rsynk.settings.SshServerSettings
import org.apache.sshd.common.keyprovider.KeyPairProvider
import java.io.File
import java.util.*

class Rsynk internal constructor(private val builder: RsynkBuilder) : AutoCloseable {

    companion object Builder {
        fun newBuilder() = RsynkBuilder.default
    }

    private val server: RsynkSshServer
    private val trackedFiles = HashMap<String, RsynkFile>()

    private val filesProvider = object : TrackedFilesStorage {
        override fun getTrackedFile(path: String): RsynkFile? = trackedFiles[path]
        override fun getTrackedFiles(): List<RsynkFile> = trackedFiles.values.toList()
    }

    private val sshSettings = object : SshServerSettings {
        override val port: Int = builder.port
        override val nioWorkers: Int = builder.nioWorkers
        override val commandWorkers: Int = builder.commandWorkers
        override val idleConnectionTimeout: Int = builder.idleConnectionTimeout
        override val maxAuthAttempts: Int = builder.maxAuthAttempts
        override val serverKeys: KeyPairProvider = builder.serverKeysProvider
        override val applicationNameNoSpaces: String = "rsynk"
    }

    private val rsyncSettings = object : RsyncSettings {
        override val rsyncPath: String = builder.rsyncPath
        override val tempDirectory: File
            get() {
                val tmpDir = File(builder.tempDirectoryPath)
                try {
                    mkdirs(tmpDir)
                } catch (t: Throwable) {
                    throw IllegalArgumentException("Cannot initialize tmp directory: ${t.message}", t)
                }
                return tmpDir
            }
    }

    init {
        server = RsynkSshServer(
                sshSettings,
                ExplicitCommandFactory(filesProvider, sshSettings, rsyncSettings),
                SSHSessionFactory()
        )

        builder.files.forEach {
            trackedFiles.put(it.file.absolutePath, it)
        }

        server.start()
    }

    fun trackFiles(files: List<RsynkFile>): Rsynk {
        files.forEach {
            this.trackedFiles.put(it.file.absolutePath, it)
        }
        return this
    }

    fun trackFile(file: RsynkFile) {
        trackFiles(listOf(file))
    }

    fun setTrackedFiles(files: List<RsynkFile>): Rsynk {
        this.trackedFiles.clear()
        trackFiles(files)
        return this
    }

    override fun close() {
        server.stop()
    }
}


