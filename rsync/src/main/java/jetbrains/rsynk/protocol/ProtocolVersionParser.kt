package jetbrains.rsynk.protocol

import jetbrains.rsynk.errors.ProtocolException

class ProtocolVersionParser(protocol: String) {

  val response: String
  val version: Int

  init {
    if (!protocol.startsWith(Constants.RSYNCD)) {
      throw ProtocolException("Expected ${Constants.RSYNCD}<version>, but was $protocol")
    }
    val clientVersion = protocol.substring(Constants.RSYNCD.length)
    version = try {
      if (clientVersion.contains(".")) {
        clientVersion.split('.')[0].toInt()
      } else {
        clientVersion.toInt()
      }
    } catch (t: Throwable) {
      throw ProtocolException("Cannot parse version \'$clientVersion\'")
    }
    /**
     * Protocols prior to 30 only output <version> alone.  The daemon expects
     * to see a similar greeting back from the client.  For protocols prior to
     * 30, an absent ".<subprotocol>" value is assumed to be 0.  For protocol
     * 30, an absent value is a fatal error.
     */
    if (version < 30) {
      response = Constants.RSYNCD + "29" + "\n"
    } else {
      response = Constants.RSYNCD + "30.0" + "\n"
    }
  }
}