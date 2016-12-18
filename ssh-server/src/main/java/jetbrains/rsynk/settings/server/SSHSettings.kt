package jetbrains.rsynk.settings.server

interface SSHSettings {
  val port: Int

  val nioWorkers: Int
  val commandWorkers: Int
  val idleTimeout: Int
  val maxAuthRequests: Int

  val applicationNameNoSpaces: String
}
