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

import jetbrains.rsynk.files.FileInfoReader
import jetbrains.rsynk.files.TrackingFilesProvider
import jetbrains.rsynk.options.RequestOptions
import jetbrains.rsynk.server.RsyncRequestDataParser


internal interface CommandsResolver {
    fun resolve(args: List<String>): Pair<Command, RequestData>?
}

internal class AllCommandsResolver(fileInfoReader: FileInfoReader,
                                   trackingFiles: TrackingFilesProvider) : CommandsResolver {

    private val commandHolders = listOf(
            RsyncCommandsResolver(fileInfoReader, trackingFiles)
    )

    override fun resolve(args: List<String>): Pair<Command, RequestData> {
        commandHolders.forEach { holder ->
            holder.resolve(args)?.let {
                return it
            }
        }
        throw CommandNotFoundException("Zero or more than one command match given args " +
                args.joinToString(prefix = "[", postfix = "]", separator = ", "))
    }

}

internal class RsyncCommandsResolver(fileInfoReader: FileInfoReader,
                                     trackingFiles: TrackingFilesProvider) : CommandsResolver {

    private val commands: Map<RsyncCommand, (RequestOptions) -> Boolean> = mapOf(
            RsyncServerSendCommand(fileInfoReader, trackingFiles) to { options ->
                options.server && options.sender && !options.daemon
            }
    )

    override fun resolve(args: List<String>): Pair<Command, RequestData>? {
        val requestData = try {
            RsyncRequestDataParser.parse(args)
        } catch (t: Throwable) {
            throw InvalidArgumentsException("Cannot parse request arguments: $args.", t)
        }
        for ((command, predicate) in commands) {
            if (predicate(requestData.options)) {
                return Pair(command, requestData)
            }
        }
        return null
    }
}

class CommandNotFoundException(message: String) : RuntimeException(message)
class InvalidArgumentsException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
