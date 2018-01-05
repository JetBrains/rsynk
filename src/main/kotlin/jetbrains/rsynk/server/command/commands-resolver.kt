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
package jetbrains.rsynk.server.command

import jetbrains.rsynk.server.command.send.RsyncServerSendCommand
import jetbrains.rsynk.server.command.send.RsyncServerSendCommandResolver
import jetbrains.rsynk.rsync.files.FileInfoReader
import jetbrains.rsynk.rsync.files.TrackedFilesProvider


internal interface CommandArgumentsMatcher {
    fun matches(args: List<String>): Boolean
}

internal interface CommandFactory {
    fun create(): Command
}

internal class CommandResolver(fileInfoReader: FileInfoReader,
                               trackedFiles: TrackedFilesProvider
) {

    private val rsyncCommandsResolver = RsyncCommandResolver(fileInfoReader, trackedFiles)

    fun resolve(args: List<String>): Command {

        /* rsync commands */
        args.firstOrNull()?.let {
            if (it == "rsync") {
                return rsyncCommandsResolver.resolveCommand(args)
            }
        }

        throw CommandNotFoundException("Not commands found matching given arguments " +
                args.joinToString(prefix = "[", postfix = "]", separator = ", "))
    }

}

private class RsyncCommandResolver(fileInfoReader: FileInfoReader,
                                   trackedFiles: TrackedFilesProvider
) {

    private val serverSendCommandFactory = object : CommandFactory {
        override fun create(): Command {
            return RsyncServerSendCommand(fileInfoReader, trackedFiles)
        }
    }

    private val matchers: List<Pair<CommandArgumentsMatcher, CommandFactory>> = listOf(
            Pair(RsyncServerSendCommandResolver(), serverSendCommandFactory)
    )

    fun resolveCommand(args: List<String>): Command {

        val matchedCommands = matchers.filter { it.first.matches(args) }

        if (matchedCommands.isEmpty() || matchedCommands.size > 1) {
            throw CommandNotFoundException("Zero or more than one command match given args "
                    + args.joinToString(prefix = "[", postfix = "]", separator = ", "))
        }

        return matchedCommands.single().second.create()
    }
}

internal class CommandNotFoundException(message: String) : RuntimeException(message)
