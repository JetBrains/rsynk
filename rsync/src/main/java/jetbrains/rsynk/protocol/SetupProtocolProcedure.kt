package jetbrains.rsynk.protocol

class SetupProtocolProcedure(private val options: Set<Option>) {

  val response: String

  init {
   response = ""
  }
}