package jetbrains.rsynk.server

import jetbrains.rsynk.command.RequestData
import jetbrains.rsynk.data.ChecksumUtil
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
                            files.add(arg)
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
        val seed = (seedOption as? Option.ChecksumSeed)?.seed ?: ChecksumUtil.newSeed()
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
                'o' -> Option.PreserveUser
                'r' -> Option.FileSelection.Recurse
                'R' -> Option.RelativePaths
                's' -> Option.ProtectArgs
                'v' -> Option.VerboseMode
                'x' -> Option.OneFileSystem
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
            "specials" -> Option.PreserveSpecials

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
