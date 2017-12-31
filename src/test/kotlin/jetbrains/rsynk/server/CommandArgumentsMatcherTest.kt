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

import jetbrains.rsynk.rsync.files.FileInfoReader
import jetbrains.rsynk.rsync.files.RsynkFile
import jetbrains.rsynk.rsync.files.TrackedFilesProvider
import jetbrains.rsynk.rsync.files.UnixDefaultFileSystemInfo
import jetbrains.rsynk.server.command.CommandNotFoundException
import jetbrains.rsynk.server.command.CommandResolver
import org.junit.Test

class CommandArgumentsMatcherTest {

    private val rsyncCommandsHolder = CommandResolver(
            FileInfoReader(UnixDefaultFileSystemInfo()),
            object : TrackedFilesProvider {
                override fun getTrackedFiles(): List<RsynkFile> = emptyList()
            }
    )

    @Test(expected = CommandNotFoundException::class)
    fun empty_args_list_does_not_match_any_command_test() {
        rsyncCommandsHolder.resolve(emptyList())
    }

    @Test
    fun can_resolve_rsync_server_send_command_test() {
        val args = listOf("rsync", "--server", "--sender", ".")
        rsyncCommandsHolder.resolve(args)
    }

    @Test(expected = CommandNotFoundException::class)
    fun do_not_resolve_server_send_command_for_client_in_daemon_mode_test() {
        val args = listOf("rsync", "--server", "--sender", "--daemon", ".")
        rsyncCommandsHolder.resolve(args)
    }
}
