package jetbrains.rsynk.server

import jetbrains.rsynk.server.application.Rsynk
import org.junit.rules.ExternalResource
import java.util.concurrent.TimeUnit

class RsynkResource : ExternalResource() {
    val port: Int = IntegrationTestTools.findFreePort()

    val rsynk = Rsynk.builder
            .setPort(port)
            .setNumberOfWorkerThreads(1)
            .setRSAKey(IntegrationTestTools.getPrivateServerKey(), IntegrationTestTools.getPublicServerKey())
            .setIdleConnectionTimeout(IntegrationTestTools.getIdleConnectionTimeout(), TimeUnit.MILLISECONDS)
            .setNumberOfNioWorkers(1)
            .build()

    override fun after() {
        rsynk.close()
    }
}
