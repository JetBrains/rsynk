package jetbrains.rsynk.options

sealed class Option {
    object Server : Option()
    object Sender : Option()
    object Daemon : Option()

    object Compress : Option()
    object ChecksumSeedOrderFix : Option()
    object Delete : Option()

    object FListIOErrorSafety: Option()
    object IncrementalRecurse : Option()
    object RelativePaths : Option()
    object ShellCommand : Option()
    object SymlinkTimeSetting: Option()
    object OneFileSystem: Option()
    class PreReleaseInfo(val info: String) : Option()
    object ProtectArgs: Option()

    sealed class FileSelection : Option() {
        /**
         * Transfer client's file list exactly but exclude directories
         * */
        object NoDirectories : FileSelection()

        /**
         * Transfer client's file list exactly and include directories
         * recursively
         * */
        object TransferDirectoriesRecurse : FileSelection()

        /**
         * Transfer client's file list and the content of any dot directory
         * */
        object TransferDirectoriesWithoutContent : FileSelection()
    }

    object VerboseMode : Option()
}






