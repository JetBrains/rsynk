package jetbrains.rsynk.command.data

import jetbrains.rsynk.protocol.CompatFlag

data class ProtocolVersionAndFlags(val protocolVersion: Byte, val compatFlags: Set<CompatFlag>)