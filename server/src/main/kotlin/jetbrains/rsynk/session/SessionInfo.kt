package jetbrains.rsynk.session

import jetbrains.rsynk.checksum.Checksum

class SessionInfo(val options: SessionOptions,
                  val files: List<String>) {
    val checskumSeed = Checksum.nextSeed()
}
