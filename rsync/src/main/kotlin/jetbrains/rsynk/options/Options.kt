package jetbrains.rsynk.options

sealed class Option {
    object Delete : Option()
    object IncrementalRecurse : Option()
    object PreserveDevices : Option()
    object PreserveLinks : Option()
    object PreservePermissions : Option()
    object PreserveSpecials : Option()
    object PreserveTimes : Option()
    object PreserveUser : Option()
    object PreserveGroup : Option()
    object UseNumericId : Option()
    object IgnoreTimes : Option()
    object SafeFileList : Option()
    sealed class FileSelection : Option() {
        /**
         * Transfer client's file list exactly but exclude directories
         * */
        object Exact : FileSelection()

        /**
         * Transfer client's file list exactly and include directories
         * recursively
         * */
        object Recurse : FileSelection()

        /**
         * Transfer client's file list and the content of any dot directory
         * */
        object TransferDirectories : FileSelection()
    }
}






