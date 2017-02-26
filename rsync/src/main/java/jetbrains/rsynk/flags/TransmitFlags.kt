package jetbrains.rsynk.flags

import java.io.File

sealed class TransmitFlags(val value: Int) {
  object XMIT_TOP_DIR : TransmitFlags(1)
  object XMIT_SAME_MODE : TransmitFlags(2)
  object XMIT_EXTENDED_FLAGS : TransmitFlags(4)
  object XMIT_SAME_UID : TransmitFlags(8)
  object XMIT_SAME_GID : TransmitFlags(16)
  object XMIT_SAME_NAME : TransmitFlags(32)
  object XMIT_LONG_NAME : TransmitFlags(64)
  object XMIT_SAME_TIME : TransmitFlags(128)
  object XMIT_SAME_RDEV_MAJOR : TransmitFlags(256)    /* protocols 28 - now (devices only) */
  object XMIT_NO_CONTENT_DIR : TransmitFlags(256)    /* protocols 30 - now (dirs only) */
  object XMIT_HLINKED : TransmitFlags(512)
  object XMIT_USER_NAME_FOLLOWS : TransmitFlags(1024)
  object XMIT_GROUP_NAME_FOLLOWS : TransmitFlags(2048)
  object XMIT_HLINK_FIRST : TransmitFlags(4096)    /* protocols 30 - now (HLINKED files only) */
  object XMIT_IO_ERROR_ENDLIST : TransmitFlags(4096)    /* protocols 31*- now (w/XMIT_EXTENDED_FLAGS) (also protocol 30 w/'f' compat flag) */
  object XMIT_MOD_NSEC : TransmitFlags(8192)
}

//TODO: encode
val File.flags: Set<TransmitFlags>
  get() = emptySet()

//TODO: get rid of duplicate (in CompatFlags.kt)!
fun Set<TransmitFlags>.encode(): Int {
  return this.fold(0, { value, flag -> value.or(flag.value) })
}
