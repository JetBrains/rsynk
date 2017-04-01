package jetbrains.rsynk.server

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import jetbrains.rsynk.application.Rsynk
import org.apache.sshd.common.util.io.IoUtils
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import java.io.ByteArrayOutputStream

@Suppress("UsePropertyAccessSyntax")
class RsyncServerSenderSetupProtocolTest {

    companion object {
        @JvmStatic
        val rsynk = Rsynk(IntegrationTestTools.findFreePort(), 1, 1, 100, IntegrationTestTools.getServerKey())

        val jsch = JSch()

        @BeforeClass
        @JvmStatic
        fun startServer() = rsynk.start()

        @BeforeClass
        @JvmStatic
        fun stopServer() = rsynk.stop()

    }

    @Test
    fun receive_proper_code_and_message_if_sent_protocol_version_is_too_old_test() {
        val session = jsch.getSession("voytovichs", "localhost", rsynk.port)
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
        val session = jsch.getSession("voytovichs", "localhost", rsynk.port)
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
}
