package jetbrains.rsynk.command

import jetbrains.rsynk.exitvalues.ArgsParingException
import jetbrains.rsynk.options.Option
import jetbrains.rsynk.options.RequestOptions
import java.util.*


object RsyncRequestDataParser {

    private enum class State { RSYNC, OPTION, FILE }

    fun parse(args: List<String>): RequestData {

        val options = HashSet<Option>()
        val files = ArrayList<String>()
        var nextArg = State.RSYNC

        args.forEach { arg ->
            when (nextArg) {

                State.RSYNC -> {
                    if (arg != "rsync") {
                        throw ArgsParingException("'rsync' argument must be sent first")
                    }
                    nextArg = State.OPTION
                }

                State.OPTION -> {
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
                            nextArg = State.FILE
                        }
                    }
                }

                State.FILE -> {
                    files.add(arg)
                }
            }
        }
        val seedOption = options.firstOrNull { it is Option.ChecksumSeed } ?: return RequestData(RequestOptions(options), files)
        return RequestData(RequestOptions(options), files, (seedOption as Option.ChecksumSeed).seed)
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
                'L' -> Option.SymlinkTimeSetting
                'r' -> Option.FileSelection.TransferDirectoriesRecurse
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

            "one-file-system" -> Option.OneFileSystem
            "protect-args" -> Option.ProtectArgs
            else -> {
                if (o.startsWith("--checksum-seed")) {
                    return Option.ChecksumSeed(o.substring("--checksum-seed=".length).toInt())
                }
                throw ArgsParingException("Unknown long named option '$o'")
            }
        }
    }
}
