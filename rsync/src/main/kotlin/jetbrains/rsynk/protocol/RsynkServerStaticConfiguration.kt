package jetbrains.rsynk.protocol

import jetbrains.rsynk.flags.CompatFlag

object RsynkServerStaticConfiguration {
    val clientProtocolVersionMin = 31
    val serverProtocolVersion = 31
    val clientProtocolVersionMax = 31
    val serverCompatFlags: Set<CompatFlag> = emptySet() //TODO: set server flags
    val fileListPartitionLimit = 1024
    val chunkSize = 8 * 1024
}
