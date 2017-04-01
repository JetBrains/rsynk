package jetbrains.rsynk

import jetbrains.rsynk.files.FileInfoReader
import jetbrains.rsynk.files.UnixDefaultFileSystemInfo
import org.junit.Assert
import org.junit.Assume
import org.junit.Test
import java.nio.file.Files

class FileInfoReaderTest {

    private val unixFS = UnixDefaultFileSystemInfo()

    @Test
    fun `read directory info test`() {
        Assume.assumeFalse(OS.isWindows)

        val dirPath = Files.createTempDirectory("test-dir")
        val reader = FileInfoReader(unixFS)
        val dirFileInfo = reader.getFileInfo(dirPath)

        Assert.assertTrue(dirFileInfo.isDirectory)

        Assert.assertFalse(dirFileInfo.isBlockDevice)
        Assert.assertFalse(dirFileInfo.isSocket)
        Assert.assertFalse(dirFileInfo.isSymlink)
        Assert.assertFalse(dirFileInfo.isBlockDevice)
        Assert.assertFalse(dirFileInfo.isCharacterDevice)
        Assert.assertFalse(dirFileInfo.isFIFO)
        Assert.assertFalse(dirFileInfo.isReqularFile)
    }

    @Test
    fun `read file info test`() {
        Assume.assumeFalse(OS.isWindows)

        val dirPath = Files.createTempFile("test-file", ".hoho")
        val reader = FileInfoReader(unixFS)
        val dirFileInfo = reader.getFileInfo(dirPath)

        Assert.assertTrue(dirFileInfo.isReqularFile)

        Assert.assertFalse(dirFileInfo.isBlockDevice)
        Assert.assertFalse(dirFileInfo.isSocket)
        Assert.assertFalse(dirFileInfo.isSymlink)
        Assert.assertFalse(dirFileInfo.isBlockDevice)
        Assert.assertFalse(dirFileInfo.isCharacterDevice)
        Assert.assertFalse(dirFileInfo.isFIFO)
        Assert.assertFalse(dirFileInfo.isDirectory)
    }
}
