package jetbrains.rsynk.options

class RequestOptions(val options: Set<Option>) {

    val server: Boolean = Option.Server in options
    val sender: Boolean = Option.Sender in options
    val daemon: Boolean = Option.Daemon in options

    val compress: Boolean = Option.Compress in options
    val checksumSeedOrderFix = Option.ChecksumSeedOrderFix in options

    val delete: Boolean = Option.Delete in options
    val filesSelection: Option.FileSelection
        get() {
            return options.filter { it is Option.FileSelection }
                    .map { it as Option.FileSelection }
                    .singleOrNull() ?: Option.FileSelection.NoDirectories
        }

    val numericIds: Boolean = Option.NumericIds in options
    val oneFileSystem: Boolean = Option.OneFileSystem in options
    val preReleaseInfo: String?
        get() {
            val info = options.firstOrNull { it is Option.PreReleaseInfo } as? Option.PreReleaseInfo
            return info?.info
        }
    val preserveDevices: Boolean  = Option.PreserveDevices in options
    val preserveGroup: Boolean = Option.PreserveGroup in options
    val preserveLinks: Boolean = Option.PreserveLinks in options
    val preserveSpecials: Boolean = Option.PreserveSpecials in options
    val preserveUser: Boolean = Option.PreserveUser in options
    val protectArgs: Boolean = Option.ProtectArgs in options
    val relativeNames: Boolean = Option.RelativePaths in options
    val saveFlist: Boolean = Option.FListIOErrorSafety in options
    val shellCommand: Boolean = Option.ShellCommand in options
    val symlinkTimeSettings: Boolean = Option.SymlinkTimeSetting in options
    val verboseMode: Boolean = Option.VerboseMode in options
}
