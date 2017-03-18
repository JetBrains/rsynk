package jetbrains.rsynk.command


interface CommandsHolder {
    fun resolve(args: List<String>): Command
}

internal class RsyncCommandsHolder : CommandsHolder {

    private val commands: List<RsyncCommand> = listOf(
            RsyncServerSendCommand()
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
        return this.matchArgs.zip(args).all { it.first == it.second }
    }
}

internal class CommandNotFoundException(message: String) : RuntimeException(message)
