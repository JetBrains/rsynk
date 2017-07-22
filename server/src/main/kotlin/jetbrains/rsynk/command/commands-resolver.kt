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
package jetbrains.rsynk.command

import jetbrains.rsynk.files.TrackedFilesStorage
import jetbrains.rsynk.settings.RsyncSettings


internal interface CommandsResolver {
    fun resolve(args: List<String>): Command
}

internal class AllCommandsResolver(trackedFiles: TrackedFilesStorage,
                                   rsyncSettings: RsyncSettings
) : CommandsResolver {

    private val rsyncCommandsResolver = RsyncCommandsResolver(trackedFiles, rsyncSettings)

    override fun resolve(args: List<String>): Command {

        args.firstOrNull()?.let {
            if (it == "rsync") {
                return rsyncCommandsResolver.resolve(args)
            }
        }

        throw CommandNotFoundException("Not commands found matching given arguments " +
                args.joinToString(prefix = "[", postfix = "]", separator = ", "))
    }

}

internal class RsyncCommandsResolver(trackedFiles: TrackedFilesStorage,
                                     rsyncSettings: RsyncSettings
) : CommandsResolver {

    private val commands: List<RsyncCommand> = listOf(
            RsyncServerSendCommand(trackedFiles, rsyncSettings)
    )

    override fun resolve(args: List<String>): Command {

        val matchedCommands = commands.filter { it.matchArguments(args) }

        if (matchedCommands.isEmpty() || matchedCommands.size > 1) {
            throw CommandNotFoundException("Zero or more than one command match given args "
                    + args.joinToString(prefix = "[", postfix = "]", separator = ", "))
        }

        return matchedCommands[0]
    }
}

class CommandNotFoundException(message: String) : RuntimeException(message)
