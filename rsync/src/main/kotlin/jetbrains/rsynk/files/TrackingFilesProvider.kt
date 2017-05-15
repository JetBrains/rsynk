package jetbrains.rsynk.files

class TrackingFilesProvider(private val fetchTrackingFiles: () -> List<RsynkFile>) {
    fun getTrackkngFiles() = fetchTrackingFiles()
}
