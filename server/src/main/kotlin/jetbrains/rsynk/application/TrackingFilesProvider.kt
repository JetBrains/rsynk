package jetbrains.rsynk.application

internal class TrackingFilesProvider(private val fetchTrackingFiles: () -> List<RsynkFile>) {
    fun getTrackkngFiles() = fetchTrackingFiles()
}
