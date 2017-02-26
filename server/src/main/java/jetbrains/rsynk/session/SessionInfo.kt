package jetbrains.rsynk.session

import jetbrains.rsynk.checksum.Checksum

class SessionInfo(val options: SessionOptions) {
  val checskumSeed = Checksum.nextSeed()
}
