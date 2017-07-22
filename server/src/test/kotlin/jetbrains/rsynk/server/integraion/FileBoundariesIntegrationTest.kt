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
package jetbrains.rsynk.server.integraion

import jetbrains.rsynk.application.Rsynk
import jetbrains.rsynk.files.RsynkFile
import jetbrains.rsynk.files.RsynkFileBoundaries
import jetbrains.rsynk.server.IntegrationTestTools
import jetbrains.rsynk.server.RsyncCommand
import org.junit.AfterClass
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.nio.file.Files

class FileBoundariesIntegrationTest {

    @Test
    fun set_left_file_bound_test() {
        val dataDirectory = Files.createTempDirectory("data-${RsyncIntegrationTest.id.incrementAndGet()}").toFile()
        val sourceFile = File(dataDirectory, "from.txt")
        sourceFile.writeText(IntegrationTestTools.loremIpsum)

        val destinationDirectory = Files.createTempDirectory("data-${RsyncIntegrationTest.id.incrementAndGet()}").toFile()
        val destinationFile = File(destinationDirectory, "to.txt")
        Assert.assertTrue("Cannot create new file", destinationFile.createNewFile())

        rsynk.addTrackingFile(RsynkFile(sourceFile) {
            RsynkFileBoundaries(10, IntegrationTestTools.loremIpsum.length.toLong() - 10)
        })

        RsyncCommand.sync("localhost:${sourceFile.absolutePath}", destinationFile.absolutePath, RsyncIntegrationTest.freePort, 10, "v")
        Assert.assertEquals(IntegrationTestTools.loremIpsum.substring(10), destinationFile.readText())
    }

    @Test
    fun set_right_file_bound_test() {
        val dataDirectory = Files.createTempDirectory("data-${RsyncIntegrationTest.id.incrementAndGet()}").toFile()
        val sourceFile = File(dataDirectory, "from.txt")
        sourceFile.writeText(IntegrationTestTools.loremIpsum)

        val destinationDirectory = Files.createTempDirectory("data-${RsyncIntegrationTest.id.incrementAndGet()}").toFile()
        val destinationFile = File(destinationDirectory, "to.txt")
        Assert.assertTrue("Cannot create new file", destinationFile.createNewFile())

        rsynk.addTrackingFile(RsynkFile(sourceFile) {
            RsynkFileBoundaries(0, 20)
        })

        RsyncCommand.sync("localhost:${sourceFile.absolutePath}", destinationFile.absolutePath, RsyncIntegrationTest.freePort, 10, "v")
        Assert.assertEquals(IntegrationTestTools.loremIpsum.substring(0, 20), destinationFile.readText())
    }

    @Test
    fun set_left_and_right_bounds_test() {
        val dataDirectory = Files.createTempDirectory("data-${RsyncIntegrationTest.id.incrementAndGet()}").toFile()
        val sourceFile = File(dataDirectory, "from.txt")
        sourceFile.writeText(IntegrationTestTools.loremIpsum)

        val destinationDirectory = Files.createTempDirectory("data-${RsyncIntegrationTest.id.incrementAndGet()}").toFile()
        val destinationFile = File(destinationDirectory, "to.txt")
        Assert.assertTrue("Cannot create new file", destinationFile.createNewFile())

        rsynk.addTrackingFile(RsynkFile(sourceFile) {
            RsynkFileBoundaries(10, 30)
        })

        RsyncCommand.sync("localhost:${sourceFile.absolutePath}", destinationFile.absolutePath, RsyncIntegrationTest.freePort, 10, "v")
        Assert.assertEquals(IntegrationTestTools.loremIpsum.substring(10, 40), destinationFile.readText())
    }

    @Test
    fun can_change_bounds_dynamically_test() {
        val dataDirectory = Files.createTempDirectory("data-${RsyncIntegrationTest.id.incrementAndGet()}").toFile()
        val sourceFile = File(dataDirectory, "from.txt")
        sourceFile.writeText(IntegrationTestTools.loremIpsum)

        val destinationDirectory = Files.createTempDirectory("data-${RsyncIntegrationTest.id.incrementAndGet()}").toFile()
        val destinationFile = File(destinationDirectory, "to.txt")
        Assert.assertTrue("Cannot create new file", destinationFile.createNewFile())

        rsynk.addTrackingFile(RsynkFile(sourceFile) {
            RsynkFileBoundaries(0, 10)
        })

        RsyncCommand.sync("localhost:${sourceFile.absolutePath}", destinationFile.absolutePath, RsyncIntegrationTest.freePort, 10, "v")
        Assert.assertEquals(IntegrationTestTools.loremIpsum.substring(0, 10), destinationFile.readText())


        rsynk.setTrackingFiles(emptyList()).addTrackingFile(RsynkFile(sourceFile) {
            RsynkFileBoundaries(10, 20)
        })

        RsyncCommand.sync("localhost:${sourceFile.absolutePath}", destinationFile.absolutePath, RsyncIntegrationTest.freePort, 10, "v")
        Assert.assertEquals(IntegrationTestTools.loremIpsum.substring(10, 20), destinationFile.readText())
    }

    companion object {
        val freePort = IntegrationTestTools.findFreePort()

        @JvmStatic
        val rsynk = Rsynk.newBuilder().apply {
            port = freePort
            nioWorkers = 1
            commandWorkers = 1
            idleConnectionTimeout = 30000
            serverKeysProvider = IntegrationTestTools.getServerKey()
        }.build()

        @AfterClass
        @JvmStatic
        fun stopServer() = rsynk.close()
    }
}
