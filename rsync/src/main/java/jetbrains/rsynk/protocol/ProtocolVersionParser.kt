package jetbrains.rsynk.protocol

import jetbrains.rsynk.exitvalues.ProtocolException
import jetbrains.rsynk.exitvalues.UnsupportedProtocolException

class ProtocolVersionParser(protocol: String) {

  val rsyncFormattedProtocolVersion: String
  val protocolVersion: Int
  val subProtocolVersion: Int

  init {
    if (!protocol.startsWith(Constants.RSYNCD)) {
      throw ProtocolException("Expected ${Constants.RSYNCD}<version>, but was $protocol")
    }
    val clientVersion = protocol.substring(Constants.RSYNCD.length)
    val version = try {
      if (clientVersion.count { it == '.' } > 1) {
        throw ProtocolException("")
      }
      if (clientVersion.contains(".")) {
        val aPair = clientVersion.split('.')
        Pair(aPair[0].toInt(), aPair[1].toInt())
      } else {
        Pair(clientVersion.toInt(), 0)
      }
    } catch (t: Throwable) {
      throw ProtocolException("Cannot parse version \'$clientVersion\'")
    }
    protocolVersion = version.first
    subProtocolVersion = version.second
    /**
     * Protocols prior to 30 only output <version> alone.  The daemon expects
     * to see a similar greeting back from the client.  For protocols prior to
     * 30, an absent ".<subprotocol>" value is assumed to be 0.  For protocol
     * 30, an absent value is a fatal error.
     */
    if (protocolVersion < 30) {
      rsyncFormattedProtocolVersion = Constants.RSYNCD + "29" + "\n"
    } else {
      rsyncFormattedProtocolVersion = Constants.RSYNCD + "30.0" + "\n"
    }
    if (protocolVersion < 26) {
      throw UnsupportedProtocolException("Protocols older than 26 (client has $protocolVersion) is not supported")
    }
  }
}