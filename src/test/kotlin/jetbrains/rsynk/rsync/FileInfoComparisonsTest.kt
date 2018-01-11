/**
 * Copyright 2016 - 2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jetbrains.rsynk.rsync

import jetbrains.rsynk.rsync.files.FileInfo
import jetbrains.rsynk.rsync.files.Group
import jetbrains.rsynk.rsync.files.User
import org.junit.Assert
import org.junit.Test
import java.nio.file.Paths

internal class FileInfoComparisonsTest {
    @Test
    fun dot_dir_is_less_than_file_test() {
        val dot = FileInfo(Paths.get("."), 0, 0, 0L, 0L, user, group)
        val file = FileInfo(Paths.get("aaa/bbb"), regularFileMode, 0, 0L, 0L, user, group)
        Assert.assertTrue(dot < file)
    }

    @Test
    fun dot_dir_is_less_than_dir_test() {
        val dot = FileInfo(Paths.get("."), 0, 0, 0L, 0L, user, group)
        val dir = FileInfo(Paths.get("aaa/bbb"), directoryMode, 0, 0L, 0L, user, group)
        Assert.assertTrue(dot < dir)
    }

    @Test
    fun file_is_bigger_than_dot_dir_test() {
        val file = FileInfo(Paths.get("haha/hoho"), regularFileMode, 0, 0L, 0L, user, group)
        val dot = FileInfo(Paths.get("."), 0, 0, 0L, 0L, user, group)
        Assert.assertTrue(file > dot)
    }

    @Test
    fun dir_is_bigger_that_dot_dir_test() {
        val dir = FileInfo(Paths.get("direc/tory"), directoryMode, 0, 0L, 0L, user, group)
        val dot = FileInfo(Paths.get("."), 0, 0, 0L, 0L, user, group)
        Assert.assertTrue(dir > dot)
    }

    @Test
    fun compare_directories_same_path_test() {
        val dir1 = FileInfo(Paths.get("aa/aa"), directoryMode, 0, 0L, 0L, user, group)
        val dir2 = FileInfo(Paths.get("aa/aa"), directoryMode, 0, 0L, 0L, user, group)
        Assert.assertTrue(dir1 == dir2)
    }

    @Test
    fun compare_directories_same_path_length_test() {
        val dir1 = FileInfo(Paths.get("aa/aa"), directoryMode, 0, 0L, 0L, user, group)
        val dir2 = FileInfo(Paths.get("aa/ab"), directoryMode, 0, 0L, 0L, user, group)
        Assert.assertTrue(dir1 < dir2)
    }

    @Test
    fun compare_directories_left_is_shorter_and_smaller_test() {
        val dir1 = FileInfo(Paths.get("aa/a"), directoryMode, 0, 0L, 0L, user, group)
        val dir2 = FileInfo(Paths.get("aa/bb"), directoryMode, 0, 0L, 0L, user, group)
        Assert.assertTrue(dir1 < dir2)
    }

    @Test
    fun compare_directories_left_is_shorter_and_bigger_test() {
        val dir1 = FileInfo(Paths.get("ab/a"), directoryMode, 0, 0L, 0L, user, group)
        val dir2 = FileInfo(Paths.get("aa/bb"), directoryMode, 0, 0L, 0L, user, group)
        Assert.assertTrue(dir1 > dir2)
    }

    @Test
    fun compare_directories_left_is_longer_and_smaller_test() {
        val dir1 = FileInfo(Paths.get("aa/aaaa"), directoryMode, 0, 0L, 0L, user, group)
        val dir2 = FileInfo(Paths.get("aa/ba"), directoryMode, 0, 0L, 0L, user, group)
        Assert.assertTrue(dir1 < dir2)
    }

    @Test
    fun compare_directories_left_is_longer_and_bigger_test() {
        val dir1 = FileInfo(Paths.get("ab/aaaa"), directoryMode, 0, 0L, 0L, user, group)
        val dir2 = FileInfo(Paths.get("aa/ba"), directoryMode, 0, 0L, 0L, user, group)
        Assert.assertTrue(dir1 > dir2)
    }

    @Test
    fun compare_files_same_path_test_test() {
        val file1 = FileInfo(Paths.get("hoho/haha.ha"), regularFileMode, 0, 0L, 0L, user, group)
        val file2 = FileInfo(Paths.get("hoho/haha.ha"), regularFileMode, 0, 0L, 0L, user, group)
        Assert.assertTrue(file1 == file2)
    }

    @Test
    fun compare_files_same_path_length_test() {
        val file1 = FileInfo(Paths.get("hoho/haha.za"), regularFileMode, 0, 0L, 0L, user, group)
        val file2 = FileInfo(Paths.get("hoho/haha.ha"), regularFileMode, 0, 0L, 0L, user, group)
        Assert.assertTrue(file1 > file2)
    }

    @Test
    fun compare_files_left_is_shorter_and_smaller_test() {
        val file1 = FileInfo(Paths.get("hoho/aa.ha"), regularFileMode, 0, 0L, 0L, user, group)
        val file2 = FileInfo(Paths.get("hoho/haha.ha"), regularFileMode, 0, 0L, 0L, user, group)
        Assert.assertTrue(file1 < file2)
    }

    @Test
    fun compare_files_left_is_shorter_and_bigger_test() {
        val file1 = FileInfo(Paths.get("hoho/aa.ha"), regularFileMode, 0,  0L, 0L, user, group)
        val file2 = FileInfo(Paths.get("hoha/haha.ha"), regularFileMode, 0, 0L, 0L, user, group)
        Assert.assertTrue(file1 > file2)
    }

    @Test
    fun compare_files_left_is_longer_and_smaller_test() {
        val file1 = FileInfo(Paths.get("hoho/aaaaaaaaa.ha"), regularFileMode, 0, 0L, 0L, user, group)
        val file2 = FileInfo(Paths.get("hoho/haha.ha"), regularFileMode, 0, 0L, 0L, user, group)
        Assert.assertTrue(file1 < file2)
    }

    @Test
    fun compare_files_left_is_longer_and_bigger_test() {
        val file1 = FileInfo(Paths.get("hoho/aaaaaaaaa.ha"), regularFileMode, 0, 0L, 0L, user, group)
        val file2 = FileInfo(Paths.get("hoha/haha.ha"), regularFileMode, 0, 0L, 0L, user, group)
        Assert.assertTrue(file1 > file2)
    }

    @Test
    fun file_is_always_less_than_directory_test() {
        val file = FileInfo(Paths.get("hoho/aa.ha"), regularFileMode, 0, 0L, 0L, user, group)
        val dir = FileInfo(Paths.get("aaa/haha"), directoryMode, 0, 0L, 0L, user, group)
        Assert.assertTrue(file < dir)
    }

    private val user = User("test", 42)
    private val group = Group("test", 42)
    private val regularFileMode = 33416
    private val directoryMode = 17161
}
