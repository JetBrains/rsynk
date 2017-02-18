package jetbrains.rsynk.application

import jetbrains.rsynk.protocol.CompatFlag

data class ServerCompatFlagsHolder(val compatFlags: Set<CompatFlag>)