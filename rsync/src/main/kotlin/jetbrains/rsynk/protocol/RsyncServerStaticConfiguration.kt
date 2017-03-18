package jetbrains.rsynk.protocol

import jetbrains.rsynk.flags.CompatFlag

object RsyncServerStaticConfiguration {
    val clientProtocolVersionMin = 31
    val serverProtocolVersion = 31
    val clientProtocolVersionMax = 31
    val serverCompatFlags: Set<CompatFlag> = emptySet() //TODO: set server flags
}
