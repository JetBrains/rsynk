package jetbrains.rsynk.session

import jetbrains.rsynk.options.Option

class SessionOptions(val options: Set<Option>) {
  val delete: Boolean
    get() = options.contains(Option.Delete)
  //TODO: finish implementation
}
