package jetbrains.rsynk.protocol

import jetbrains.rsynk.files.Modules

class ListAllModules(private val modules: Modules) {
  val response: String
    get() {
      val response = StringBuilder()
      modules.listModules().forEach { module ->
        response.append("${module.name}\t${module.comment}\n")
      }
      response.append(Constants.RSYNCD_EXIT + "\n")
      return response.toString()
    }
}