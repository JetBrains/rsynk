package jetbrains.rsynk.flags

import java.util.*

sealed class TransmitFlags(override val value: Int) : Flag {
  object TopDirectory : TransmitFlags(1)
  object SameMode : TransmitFlags(2)
  object ExtendedFlags : TransmitFlags(4)
  object SameUserId : TransmitFlags(8)
  object SameGroupId : TransmitFlags(16)
  object SameName : TransmitFlags(32)
  object SameLongName : TransmitFlags(64)
  object SameTime : TransmitFlags(128)
  object SameRdevMajor : TransmitFlags(256)    /* protocols 28 - now (devices only) */
  object NoContentDirs : TransmitFlags(256)    /* protocols 30 - now (dirs only) */
  object HardLinked : TransmitFlags(512)
  object UserNameFollows : TransmitFlags(1024)
  object GroupNameFollows : TransmitFlags(2048)
  object HardLinksFirst : TransmitFlags(4096)    /* protocols 30 - now (HLINKED files only) */
  object IoErrorEndList : TransmitFlags(4096)    /* protocols 31*- now (w/XMIT_EXTENDED_FLAGS) (also protocol 30 w/'f' compat flag) */
  object ModNsec : TransmitFlags(8192)
}

fun Set<TransmitFlags>.encode(): Int {
  val flagSet = HashSet<Flag>()
  flagSet.addAll(this)
  return flagSet.encode()
}


