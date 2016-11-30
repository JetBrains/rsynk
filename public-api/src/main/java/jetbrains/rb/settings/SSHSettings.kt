package jetbrains.rb.settings

interface SSHSettings {
  val port: Int
  val nioWorkers: Int
  val idleTimeout: Int
  val maxAuthRequests: Int
}
