/**
 * Copyright 2016 - 2017 JetBrains s.r.o.
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
package jetbrains.rsynk.server

import jetbrains.rsynk.exitvalues.ArgsParingException
import jetbrains.rsynk.options.Option
import org.junit.Assert
import org.junit.Test

class RsyncRequestDataParserTest {

    @Test
    fun parse_long_named_options_test() {
        val data = RsyncRequestDataParser.parse(listOf("rsync", "--server", "--sender", "-LCd", "."))
        Assert.assertTrue(data.options.server)
        Assert.assertTrue(data.options.sender)
    }

    @Test
    fun parse_short_named_options_test() {
        val data = RsyncRequestDataParser.parse(listOf("rsync", "--server", "--sender", "-LCd", "."))
        Assert.assertTrue(data.options.symlinkTimeSettings)
        Assert.assertTrue(data.options.checksumSeedOrderFix)
        Assert.assertEquals(Option.FileSelection.TransferDirectoriesWithoutContent, data.options.filesSelection)
    }

    @Test
    fun parse_files_test() {
        val data = RsyncRequestDataParser.parse(listOf("rsync", "--server", "--sender", "-Ld", ".", "/path/to/first/file",
                "path/to/second-file"))
        Assert.assertEquals(data.filePaths.joinToString(), 3, data.filePaths.size)
        Assert.assertTrue(data.filePaths.contains("/path/to/first/file"))
        Assert.assertTrue(data.filePaths.contains("path/to/second-file"))
    }

    @Test
    fun include_dot_dir_as_as_file_test() {
        val data = RsyncRequestDataParser.parse(listOf("rsync", "--server", "--sender", "-Ld", ".", "/path/to/file"))
        Assert.assertEquals(data.filePaths.joinToString(), 2, data.filePaths.size)
        Assert.assertTrue(data.filePaths.contains("."))
    }

    @Test
    fun default_file_selection_status_test() {
        val data = RsyncRequestDataParser.parse(listOf("rsync"))
        Assert.assertEquals(Option.FileSelection.NoDirectories, data.options.filesSelection)
    }

    @Test
    fun parse_pre_release_info_test() {
        val data = RsyncRequestDataParser.parse(listOf("rsync", "--server", "--sender", "-Le31.100002C", "."))
        Assert.assertEquals("31.100002", data.options.preReleaseInfo)
    }

    @Test(expected = ArgsParingException::class)
    fun parse_non_rsync_command_test() {
        RsyncRequestDataParser.parse(listOf("--server", "-e.sd", "."))
    }

    @Test
    fun parse_checksum_seed_test() {
        val data = RsyncRequestDataParser.parse(listOf("rsync", "--server", "--sender", "--checksum-seed=42", "."))
        Assert.assertEquals(42, data.checksumSeed)
    }
}
