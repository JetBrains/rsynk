package jetbrains.rsynk.errors

open class RsynkException(message: String) : RuntimeException(message)

class ProtocolException(message: String) : RsynkException(message)
class UnsupportedProtocolException(message: String) : RuntimeException(message)
class ModuleNotFoundException(message: String) : RsynkException(message)