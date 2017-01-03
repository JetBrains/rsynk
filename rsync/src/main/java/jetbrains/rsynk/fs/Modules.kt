package jetbrains.rsynk.fs

import org.slf4j.LoggerFactory
import java.util.*

class Modules {
  private val log = LoggerFactory.getLogger(javaClass)
  private val modules = HashMap<String, Module>()

  fun register(module: Module) {
    log.info("New fs registered: $module")
    modules[module.name] = module
  }

  fun find(name: String): Module? = modules[name]

  fun remove(name: String): Boolean {
    return modules.remove(name) != null
  }
}