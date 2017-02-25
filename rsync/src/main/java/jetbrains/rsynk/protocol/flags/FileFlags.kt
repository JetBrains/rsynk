package jetbrains.rsynk.protocol.flags

import java.io.File

sealed class FileFlags(val value: Int) {
  object XMIT_TOP_DIR : FileFlags(1)
  object XMIT_SAME_MODE : FileFlags(2)
  object XMIT_EXTENDED_FLAGS : FileFlags(4)
  object XMIT_SAME_UID : FileFlags(8)
  object XMIT_SAME_GID : FileFlags(16)
  object XMIT_SAME_NAME : FileFlags(32)
  object XMIT_LONG_NAME : FileFlags(64)
  object XMIT_SAME_TIME : FileFlags(128)
  object XMIT_SAME_RDEV_MAJOR : FileFlags(256)    /* protocols 28 - now (devices only) */
  object XMIT_NO_CONTENT_DIR : FileFlags(256)    /* protocols 30 - now (dirs only) */
  object XMIT_HLINKED : FileFlags(512)
  object XMIT_USER_NAME_FOLLOWS : FileFlags(1024)
  object XMIT_GROUP_NAME_FOLLOWS : FileFlags(2048)
  object XMIT_HLINK_FIRST : FileFlags(4096)    /* protocols 30 - now (HLINKED files only) */
  object XMIT_IO_ERROR_ENDLIST : FileFlags(4096)    /* protocols 31*- now (w/XMIT_EXTENDED_FLAGS) (also protocol 30 w/'f' compat flag) */
  object XMIT_MOD_NSEC : FileFlags(8192)
}

//TODO: encode
val File.flags: Set<FileFlags>
  get() = emptySet()

//TODO: get rid of duplicate (in CompatFlags.kt)!
fun Set<FileFlags>.encode(): Int {
  return this.fold(0, { value, flag -> value.or(flag.value) })
}