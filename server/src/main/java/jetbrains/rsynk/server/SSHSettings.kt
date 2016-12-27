package jetbrains.rsynk.server

interface SSHSettings {
  val port: Int
  val nioWorkers: Int
  val commandWorkers: Int
  val idleConnectionTimeout: Int
  val maxAuthAttempts: Int
  val applicationNameNoSpaces: String
  val serverSSHKeyPath: String
}
