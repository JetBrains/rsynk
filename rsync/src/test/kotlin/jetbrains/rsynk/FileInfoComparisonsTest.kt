package jetbrains.rsynk

import jetbrains.rsynk.files.FileInfo
import jetbrains.rsynk.files.Group
import jetbrains.rsynk.files.User
import org.junit.Assert
import org.junit.Test
import java.nio.file.Paths

class FileInfoComparisonsTest {
    @Test
    fun `dot dir is less than file test`() {
        val dot = FileInfo(Paths.get("."), 0, 0L, 0L, user, group)
        val file = FileInfo(Paths.get("aaa/bbb"), regularFileMode, 0L, 0L, user, group)
        Assert.assertTrue(dot < file)
    }

    @Test
    fun `dot dir is less than dir test`() {
        val dot = FileInfo(Paths.get("."), 0, 0L, 0L, user, group)
        val dir = FileInfo(Paths.get("aaa/bbb"), directoryMode, 0L, 0L, user, group)
        Assert.assertTrue(dot < dir)
    }

    @Test
    fun `file is bigger than dot dir test`() {
        val file = FileInfo(Paths.get("haha/hoho"), regularFileMode, 0L, 0L, user, group)
        val dot = FileInfo(Paths.get("."), 0, 0L, 0L, user, group)
        Assert.assertTrue(file > dot)
    }

    @Test
    fun `dir is bigger that dot dir test`() {
        val dir = FileInfo(Paths.get("direc/tory"), directoryMode, 0L, 0L, user, group)
        val dot = FileInfo(Paths.get("."), 0, 0L, 0L, user, group)
        Assert.assertTrue(dir > dot)
    }

    @Test
    fun `compare directories same path test`() {
        val dir1 = FileInfo(Paths.get("aa/aa"), directoryMode, 0L, 0L, user, group)
        val dir2 = FileInfo(Paths.get("aa/aa"), directoryMode, 0L, 0L, user, group)
        Assert.assertTrue(dir1 == dir2)
    }

    @Test
    fun `compare directories same path length test`() {
        val dir1 = FileInfo(Paths.get("aa/aa"), directoryMode, 0L, 0L, user, group)
        val dir2 = FileInfo(Paths.get("aa/ab"), directoryMode, 0L, 0L, user, group)
        Assert.assertTrue(dir1 < dir2)
    }

    @Test
    fun `compare directories left is shorter and smaller test`() {
        val dir1 = FileInfo(Paths.get("aa/a"), directoryMode, 0L, 0L, user, group)
        val dir2 = FileInfo(Paths.get("aa/bb"), directoryMode, 0L, 0L, user, group)
        Assert.assertTrue(dir1 < dir2)
    }

    @Test
    fun `compare directories left is shorter and bigger test`() {
        val dir1 = FileInfo(Paths.get("ab/a"), directoryMode, 0L, 0L, user, group)
        val dir2 = FileInfo(Paths.get("aa/bb"), directoryMode, 0L, 0L, user, group)
        Assert.assertTrue(dir1 > dir2)
    }

    @Test
    fun `compare directories left is longer and smaller test`() {
        val dir1 = FileInfo(Paths.get("aa/aaaa"), directoryMode, 0L, 0L, user, group)
        val dir2 = FileInfo(Paths.get("aa/ba"), directoryMode, 0L, 0L, user, group)
        Assert.assertTrue(dir1 < dir2)
    }

    @Test
    fun `compare directories left is longer and bigger test`() {
        val dir1 = FileInfo(Paths.get("ab/aaaa"), directoryMode, 0L, 0L, user, group)
        val dir2 = FileInfo(Paths.get("aa/ba"), directoryMode, 0L, 0L, user, group)
        Assert.assertTrue(dir1 > dir2)
    }

    @Test
    fun `compare files same path test test`() {
        val file1 = FileInfo(Paths.get("hoho/haha.ha"), regularFileMode, 0L, 0L, user, group)
        val file2 = FileInfo(Paths.get("hoho/haha.ha"), regularFileMode, 0L, 0L, user, group)
        Assert.assertTrue(file1 == file2)
    }

    @Test
    fun `compare files same path length test`() {
        val file1 = FileInfo(Paths.get("hoho/haha.za"), regularFileMode, 0L, 0L, user, group)
        val file2 = FileInfo(Paths.get("hoho/haha.ha"), regularFileMode, 0L, 0L, user, group)
        Assert.assertTrue(file1 > file2)
    }

    @Test
    fun `compare files left is shorter and smaller test`() {
        val file1 = FileInfo(Paths.get("hoho/aa.ha"), regularFileMode, 0L, 0L, user, group)
        val file2 = FileInfo(Paths.get("hoho/haha.ha"), regularFileMode, 0L, 0L, user, group)
        Assert.assertTrue(file1 < file2)
    }

    @Test
    fun `compare files left is shorter and bigger test`() {
        val file1 = FileInfo(Paths.get("hoho/aa.ha"), regularFileMode, 0L, 0L, user, group)
        val file2 = FileInfo(Paths.get("hoha/haha.ha"), regularFileMode, 0L, 0L, user, group)
        Assert.assertTrue(file1 > file2)
    }

    @Test
    fun `compare files left is longer and smaller test`() {
        val file1 = FileInfo(Paths.get("hoho/aaaaaaaaa.ha"), regularFileMode, 0L, 0L, user, group)
        val file2 = FileInfo(Paths.get("hoho/haha.ha"), regularFileMode, 0L, 0L, user, group)
        Assert.assertTrue(file1 < file2)
    }

    @Test
    fun `compare files left is longer and bigger test`() {
        val file1 = FileInfo(Paths.get("hoho/aaaaaaaaa.ha"), regularFileMode, 0L, 0L, user, group)
        val file2 = FileInfo(Paths.get("hoha/haha.ha"), regularFileMode, 0L, 0L, user, group)
        Assert.assertTrue(file1 > file2)
    }

    @Test
    fun `file is always less than directory test`() {
        val file = FileInfo(Paths.get("hoho/aa.ha"), regularFileMode, 0L, 0L, user, group)
        val dir = FileInfo(Paths.get("aaa/haha"), directoryMode, 0L, 0L, user, group)
        Assert.assertTrue(file < dir)
    }

    private val user = User("test", 42)
    private val group = Group("test", 42)
    private val regularFileMode = 33416
    private val directoryMode = 17161
}
