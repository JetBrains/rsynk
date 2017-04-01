package jetbrains.rsynk


object OS {
    val isLinux: Boolean
        get() {
            val os = os()
            return os.contains("linux")
        }

    val isMac: Boolean
        get() {
            val os = os()
            return os.contains("mac") || os.contains("os x") || os.contains("darwin")
        }

    val isWindows: Boolean
        get() {
            return os().contains("windows")
        }

    private fun os() = System.getProperty("os.name")?.toLowerCase() ?: ""
}
