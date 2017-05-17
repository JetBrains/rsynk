package jetbrains.rsynk.server

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import jetbrains.rsynk.application.Rsynk
import org.apache.sshd.common.util.io.IoUtils
import org.junit.AfterClass
import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayOutputStream

@Suppress("UsePropertyAccessSyntax")
class ErrorCodesIntegrationTest {

    companion object {

        val port = IntegrationTestTools.findFreePort()

        @JvmStatic
        val rsynk = Rsynk.newBuilder().apply {
            port = port
            nioWorkers = 1
            commandWorkers = 1
            idleConnectionTimeout = 30000
            serverKeysProvider = IntegrationTestTools.getServerKey()
        }.build()

        val jsch = JSch()

        @AfterClass
        @JvmStatic
        fun stopServer() = rsynk.close()

    }

    @Test
    fun receive_proper_code_and_message_if_sent_protocol_version_is_too_old_test() {
        val session = jsch.getSession("voytovichs", "localhost", port)
        session.setConfig("StrictHostKeyChecking", "no")
        session.setTimeout(60000)
        session.setPassword("whatever".toByteArray())
        session.connect()
        val channel = session.openChannel("exec") as ChannelExec

        channel.setCommand("rsync --server --sender")
        channel.connect()

        val output = channel.outputStream
        output.write(byteArrayOf(10 /* 100% ancient protocol */, 0, 0, 0))
        output.flush()

        val bos = ByteArrayOutputStream()
        IoUtils.copy(channel.errStream, bos)
        val reportedError = String(bos.toByteArray())

        channel.disconnect()
        session.disconnect()

        Assert.assertTrue("Reported error '$reportedError' doesn't contain expected message",
                reportedError.toLowerCase().contains("client protocol version must be at least"))
        Assert.assertEquals(2 /* protocol incompatibility code */, channel.exitStatus)
    }

    @Test
    fun receive_proper_code_and_message_if_sent_protocol_version_is_too_new_test() {
        val session = jsch.getSession("voytovichs", "localhost", port)
        session.setConfig("StrictHostKeyChecking", "no")
        session.setTimeout(60000)
        session.setPassword("whatever".toByteArray())
        session.connect()
        val channel = session.openChannel("exec") as ChannelExec

        channel.setCommand("rsync --server --sender")
        channel.connect()

        val output = channel.outputStream
        output.write(byteArrayOf(47 /* 100% too modern protocol */, 0, 0, 0))
        output.flush()

        val bos = ByteArrayOutputStream()
        IoUtils.copy(channel.errStream, bos)
        val reportedError = String(bos.toByteArray())

        channel.disconnect()
        session.disconnect()

        Assert.assertTrue("Reported error '$reportedError' doesn't contain expected message",
                reportedError.toLowerCase().contains("client protocol version must be no more than"))
        Assert.assertEquals(2 /* protocol incompatibility code */, channel.exitStatus)
    }

    @Test
    fun request_not_tracked_file_test() {
        val session = jsch.getSession("voytovichs", "localhost", port)
        session.setConfig("StrictHostKeyChecking", "no")
        session.setTimeout(60000)
        session.setPassword("whatever".toByteArray())
        session.connect()
        val channel = session.openChannel("exec") as ChannelExec

        channel.setCommand("rsync --server --sender . not/existing/path")
        channel.connect()

        val output = channel.outputStream

        output.write(byteArrayOf(31, 0, 0, 0))
        output.flush()

        output.write(byteArrayOf(0, 0, 0, 0))
        output.flush()

        val bos = ByteArrayOutputStream()
        IoUtils.copy(channel.errStream, bos)
        val reportedError = String(bos.toByteArray())

        channel.disconnect()
        session.disconnect()

        Assert.assertTrue("Reported error '$reportedError' doesn't contain expected message",
                reportedError.contains("File not/existing/path is missing among files tracked by rsynk"))
        Assert.assertEquals(3 /* files selection error code */, channel.exitStatus)
    }
}
