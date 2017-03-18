package jetbrains.rsynk.options

sealed class Option(val long: String?, val short: String?) {
    object Server : Option("server", null)
    object Sender : Option("sender", null)
    object Daemon : Option("daemon", null)

    object Compress : Option(null, "z")
    object Delete : Option("delete", null)
    object DeleteBefore: Option("delete-before", null)
    object DeleteDuring: Option("delete-during", null)

    object IncrementalRecurse : Option("inc-recursive", null)
    object RelativeNames : Option("relative", "R")
    object CopyDirectoriesWithoutContent : Option("dirs", "d")
    sealed class FileSelection(long: String?, short: String?) : Option(long, short) {
        /**
         * Transfer client's file list exactly but exclude directories
         * */
        object NoDirectories : FileSelection("no-dirs", null)

        /**
         * Transfer client's file list exactly and include directories
         * recursively
         * */
        object TransferDirectoriesRecurse : FileSelection("recursive", "r")

        /**
         * Transfer client's file list and the content of any dot directory
         * */
        object TransferDirectoriesWithoutContent : FileSelection("dirs", "d")
    }
}






