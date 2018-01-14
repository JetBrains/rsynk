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
package jetbrains.rsynk.server

import jetbrains.rsynk.rsync.exitvalues.InvalidFileException
import jetbrains.rsynk.rsync.files.RsynkFile
import jetbrains.rsynk.rsync.files.RsynkFileBoundaries
import jetbrains.rsynk.server.application.TrackedFilesManager
import org.junit.Assert
import org.junit.Test

class TrackedFilesManagerTest {

    private val testFile1 = RsynkFile("hoho", { RsynkFileBoundaries(0, 1) })
    private val testFile2 = RsynkFile("hehe", { RsynkFileBoundaries(100, 500) })

    @Test
    fun add_a_file_test() {
        val t = TrackedFilesManager()
        t.add(listOf(testFile1))
        Assert.assertNotNull(t.resolve("hoho"))
    }

    @Test
    fun remove_a_file_test() {
        val t = TrackedFilesManager()
        t.add(listOf(testFile1))
        t.remove(listOf("hoho"))
        try {
            val result = t.resolve("hoho")
            Assert.fail("Removed file should not be resolved: $result")
        } catch (t: Throwable) {
            // That's what we expect
        }
    }

    @Test
    fun remove_all_files_test() {
        val t = TrackedFilesManager()
        t.add(listOf(testFile1, testFile2))
        t.resolve(listOf("hoho", "hehe"))
        t.removeAll()

        try {
            val result = t.resolve("hoho")
            Assert.fail("Removed file should not be resolved: $result")
        } catch (t: Throwable) {
            // That's what we expect
        }

        try {
            val result = t.resolve("hehe")
            Assert.fail("Removed file should not be resolved: $result")
        } catch (t: Throwable) {
            // That's what we expect
        }
    }


    @Test(expected = InvalidFileException::class)
    fun try_to_resolve_untracked_file_test() {
        val t = TrackedFilesManager()
        t.resolve("haha")
    }
}
