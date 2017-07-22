package jetbrains.rsynk.files

import jetbrains.rsynk.settings.RsyncSettings

class ThingThatCopiesTrackedFileMeaningfulPiece(private val rsyncSettings: RsyncSettings) {
    fun setupFilesForAction(files: List<RsynkFile>, action: (List<RequestedAndRealPath>) -> Unit) {
        throw UnsupportedOperationException("not implemented")
    }
}
