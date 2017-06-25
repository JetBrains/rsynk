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

import jetbrains.rsynk.command.RequestData
import jetbrains.rsynk.data.ChecksumSeedGenerator
import jetbrains.rsynk.exitvalues.ArgsParingException
import jetbrains.rsynk.options.Option
import jetbrains.rsynk.options.RequestOptions
import java.util.*


object RsyncRequestDataParser {

    private enum class ArgumentType { RSYNC, OPTION, FILE }

    fun parse(args: List<String>): RequestData {

        val options = HashSet<Option>()
        val files = ArrayList<String>()
        var nextArgType = ArgumentType.RSYNC

        args.forEach { arg ->
            when (nextArgType) {

                ArgumentType.RSYNC -> {
                    if (arg != "rsync") {
                        throw ArgsParingException("'rsync' argument must be sent first")
                    }
                    nextArgType = ArgumentType.OPTION
                }

                ArgumentType.OPTION -> {
                    when {
                        arg.isLongOption() -> {
                            options.add(parseLongName(arg))
                        }
                        arg.isShortOption() -> {
                            options.addAll(parseShortName(arg))
                        }
                        else -> {
                            if (arg != ".") {
                                throw ArgsParingException("'.' argument expected after options list, got $arg")
                            }
                            nextArgType = ArgumentType.FILE
                        }
                    }
                }

                ArgumentType.FILE -> {
                    files.add(arg)
                }
            }
        }
        val seedOption = options.firstOrNull { it is Option.ChecksumSeed }
        val seed = (seedOption as? Option.ChecksumSeed)?.seed ?: ChecksumSeedGenerator.newSeed()
        return RequestData(RequestOptions(options), files, seed)
    }

    private fun String.isShortOption(): Boolean {
        return length > 1 && startsWith("-")
    }

    private fun String.isLongOption(): Boolean {
        return length > 2 && startsWith("--")
    }

    private fun parseShortName(o: String): Set<Option> {

        val options = HashSet<Option>()

        val preReleaseInfoRegex = Regex("e\\d*\\.\\d*")
        val preReleaseInfo = preReleaseInfoRegex.find(o)?.value
        if (preReleaseInfo != null && preReleaseInfo != "e.") {
            val info = preReleaseInfo.drop(1)
            options.add(Option.PreReleaseInfo(info))
        }
        val optionToParse = o.replace(preReleaseInfoRegex, "")
        optionToParse.forEach { c ->
            val option = when (c) {

                '.' -> null
                '-' -> null

                'C' -> Option.ChecksumSeedOrderFix
                'd' -> Option.FileSelection.TransferDirectoriesWithoutContent
                'f' -> Option.FListIOErrorSafety
                'g' -> Option.PreserveGroup
                'L' -> Option.SymlinkTimeSetting
                'l' -> Option.PreserveLinks
                'm' -> Option.PruneEmptyDirectories
                'M' -> Option.PruneEmptyDirectories
                'o' -> Option.PreserveUser
                'r' -> Option.FileSelection.Recurse
                'R' -> Option.RelativePaths
                's' -> Option.ProtectArgs
                'v' -> Option.VerboseMode
                'x' -> Option.OneFileSystem
                'X' -> Option.PreserveXattrs
                'z' -> Option.Compress

                else -> throw ArgsParingException("Unknown short named option '$c' ($o)")
            }
            if (option != null) {
                options.add(option)
            }
        }
        return options
    }

    private fun parseLongName(o: String): Option {
        return when (o.dropWhile { it == '-' }) {

            "server" -> Option.Server
            "sender" -> Option.Sender
            "daemon" -> Option.Daemon

            "devices" -> Option.PreserveDevices
            "group" -> Option.PreserveGroup
            "links" -> Option.PreserveLinks
            "numeric-ids" -> Option.NumericIds
            "one-file-system" -> Option.OneFileSystem
            "owner" -> Option.PreserveUser
            "protect-args" -> Option.ProtectArgs
            "prune-empty-dirs" -> Option.PruneEmptyDirectories
            "specials" -> Option.PreserveSpecials
            "xattrs" -> Option.PreserveXattrs

            else -> {
                //special cases where == isn't enough
                if (o.startsWith("--checksum-seed")) {
                    return Option.ChecksumSeed(o.substring("--checksum-seed=".length).toInt())
                }

                throw ArgsParingException("Unknown long named option '$o'")
            }
        }
    }
}
