package jetbrains.rsynk.protocol

import jetbrains.rsynk.exitvalues.ProtocolException
import java.nio.ByteBuffer
import java.security.SecureRandom

class FinishProtocolSetupProcedure(options: Set<Option>, protocolVersion: Int) {

  val flags: Byte?
  val checksumSeed: Int = Math.abs(SecureRandom().nextInt())
  val response: ByteArray


  init {
    checkOptionsSupporting(options, protocolVersion)
    val checksumSeedBytes = ByteBuffer.allocate(4).putInt(checksumSeed).array()

    /* compat_flags required by 30 and newer */
    if (protocolVersion >= 30) {
      flags = encodeCompatFlags(options)
      response = byteArrayOf(flags) + checksumSeedBytes
    } else {
      flags = null
      response = checksumSeedBytes
    }
  }

  private fun checkOptionsSupporting(options: Set<Option>, protocolVersion: Int) {

    if (protocolVersion < 30) {
      if (options.contains(Option.PRESERVE_ACLS)) {
        throw ProtocolException("Option ${Option.PRESERVE_ACLS} requires protocol 30 or higher (negotiated $protocolVersion)")
      }
      if (options.contains(Option.PRESERVE_XATTRS)) {
        throw ProtocolException("Option ${Option.PRESERVE_XATTRS} requires protocol 30 or higher (negotiated $protocolVersion)")
      }
    }

    if (protocolVersion < 29) {
      if (options.contains(Option.FUZZY_BASIS)) {
        throw ProtocolException("Option ${Option.FUZZY_BASIS} requires protocol 29 or higher (negotiated $protocolVersion)")
      }
      if (options.contains(Option.COMPARE_DEST) && options.contains(Option.INPLACE)) {
        throw ProtocolException("${Option.COMPARE_DEST} with --${Option.INPLACE} requires protocol 29 or higher (negotiated $protocolVersion)")
      }
      if (options.contains(Option.COPY_DEST) && options.contains(Option.INPLACE)) {
        throw ProtocolException("${Option.COPY_DEST} with --${Option.INPLACE} requires protocol 29 or higher (negotiated $protocolVersion)")
      }
      if (options.contains(Option.COPY_DEST) && options.contains(Option.COMPARE_DEST)) {
        // It's not number of files must be checked but basis_dir_cnt (compat.c 248)
        // Not clear yet how it could be more than one
        throw ProtocolException("Using more than one option [${Option.COPY_DEST}, ${Option.COMPARE_DEST}] requires protocol 29 or higher (negotiated $protocolVersion)")
      }
    }
  }

  private fun encodeCompatFlags(options: Set<Option>): Byte {
    var flags: Int = 0
    if (options.contains(Option.INC_RECURSIVE)) {
      flags = 1
    }
    if (false) {
      // TODO: this is CAN_SET_SYMLINKS option
      // TODO: which is determined by rsync compilation flags
      // TODO: perhaps it should to be always set
      flags = flags.or(2)   // 1<<1
    }
    if (options.contains(Option.ICONV)) {
      flags = flags.or(4)   // 1<<2
    }
    if (options.contains(Option.FILTER)) {
      flags = flags.or(8)   // 1<<3
    }
    if (options.contains(Option.ONE_FILE_SYSTEM)) {
      flags = flags.or(16)  // 1<<4
    }
    if (options.contains(Option.CVS_EXCLUDE)) {
      flags = flags.or(32)  // 1<<5
    }
    return flags.toByte()
  }
}
