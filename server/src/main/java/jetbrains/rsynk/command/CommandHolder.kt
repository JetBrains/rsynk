package jetbrains.rsynk.command

import jetbrains.rsynk.application.ServerCompatFlagsHolder
import java.util.*


interface CommandsHolder {
  fun resolve(args: List<String>): Command
}

class RsyncCommandsHolder(serverCompatFlags: ServerCompatFlagsHolder) : CommandsHolder {

  private val commands: List<RsyncCommand> = listOf(
          RsyncServerSendCommand(serverCompatFlags)
  )

  override fun resolve(args: List<String>): Command {
    val command = commands.singleOrNull { cmd -> cmd.matches(args) } ?:
            throw CommandNotFoundException("Cannot resolve rsync command for given args [$args]")
    return command
  }

  private fun RsyncCommand.matches(args: List<String>): Boolean {
    if (args.isEmpty()) {
      return false
    }
    if (args.first() != "rsync") {
      return false
    }
    val argsSet = HashSet(args)
    return this.args.all { argsSet.contains(it) }
  }
}

class CommandNotFoundException(message: String) : RuntimeException(message)
