package jetbrains.rsynk.flags

sealed class ItemFlag(val code: Int) {
    object UpToDate : ItemFlag(0)
    object ReportAtime : ItemFlag(1 shl 0)
    object ReportChange : ItemFlag(1 shl 1)
    object ReportSize : ItemFlag(1 shl 2)         /* regular files only */
    object ReportTimeFail : ItemFlag(1 shl 2)     /* symlinks only */
    object ReportTime : ItemFlag(1 shl 3)
    object ReportPermissions : ItemFlag(1 shl 4)
    object ReportOwner : ItemFlag(1 shl 5)
    object ReportGroup : ItemFlag(1 shl 6)
    object ReportACL : ItemFlag(1 shl 7)
    object ReportXATTR : ItemFlag(1 shl 8)
    object BasicTypeFollows : ItemFlag(1 shl 11)
    object XNameFollows : ItemFlag(1 shl 12)
    object IsNew : ItemFlag(1 shl 13)
    object LocalChange : ItemFlag(1 shl 14)
    object ItemTransfer : ItemFlag(1 shl 15)
}

object ItemFlagsValidator {
    fun isFlagSupported(f: Int): Boolean = true
}
