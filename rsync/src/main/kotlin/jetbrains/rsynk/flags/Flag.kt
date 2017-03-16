package jetbrains.rsynk.flags

interface Flag {
  val value: Int
}

fun Set<Flag>.encode(): Int {
  return this.fold(0, { value, flag -> value.or(flag.value) })
}

