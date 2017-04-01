package jetbrains.rsynk.options

class RequestOptions(val options: Set<Option>) {

    val server: Boolean = options.contains(Option.Server)
    val sender: Boolean = options.contains(Option.Sender)
    val daemon: Boolean = options.contains(Option.Daemon)

    val compress: Boolean = options.contains(Option.Compress)
    val checksumSeedOrderFix = options.contains(Option.ChecksumSeedOrderFix)

    val delete: Boolean = options.contains(Option.Delete)
    val directoryMode: Option.FileSelection
        get() {
            return options.filter { it is Option.FileSelection }
                    .map { it as Option.FileSelection }
                    .singleOrNull() ?: Option.FileSelection.NoDirectories
        }

    val incrementalRecurse: Boolean = options.contains(Option.IncrementalRecurse)
    val numericIds: Boolean
        get() {
            throw UnsupportedOperationException("Not implemented")
        }
    val oneFileSystem: Boolean = options.contains(Option.OneFileSystem)
    val preReleaseInfo: String?
        get() {
            val info = options.firstOrNull { it is Option.PreReleaseInfo } as? Option.PreReleaseInfo
            return info?.info
        }
    val preserveDevices: Boolean
        get() {
            throw UnsupportedOperationException("Not implemented")
        }
    val preserveGroup: Boolean
        get() {
            throw UnsupportedOperationException("Not implemented")
        }
    val preserveLinks: Boolean
        get() {
            throw UnsupportedOperationException("Not implemented")
        }
    val preserveSpecials: Boolean
        get() {
            throw UnsupportedOperationException("Not implemented")
        }
    val preserveUser: Boolean
        get() {
            throw UnsupportedOperationException("Not implemented")
        }
    val protectArgs: Boolean = options.contains(Option.ProtectArgs)
    val relativeNames: Boolean = options.contains(Option.RelativePaths)
    val saveFlist: Boolean = options.contains(Option.FListIOErrorSafety)
    val shellCommand: Boolean = options.contains(Option.ShellCommand)
    val symlinkTimeSettings: Boolean = options.contains(Option.SymlinkTimeSetting)
    val verboseMode: Boolean = options.contains(Option.VerboseMode)
}
