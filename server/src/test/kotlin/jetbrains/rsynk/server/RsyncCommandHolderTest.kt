package jetbrains.rsynk.server

import jetbrains.rsynk.command.CommandNotFoundException
import jetbrains.rsynk.command.RsyncCommandsResolver
import jetbrains.rsynk.command.RsyncServerSendCommand
import org.junit.Test

class RsyncCommandHolderTest {

    private val rsyncCommandsHolder = RsyncCommandsResolver()

    @Test(expected = CommandNotFoundException::class)
    fun empty_args_list_does_not_match_any_command() {
        rsyncCommandsHolder.resolve(emptyList())
    }

    @Test(expected = CommandNotFoundException::class)
    fun rsync_argument_should_be_in_first_place_to_resolve_command() {
        val args = RsyncServerSendCommand().matchArgs
        rsyncCommandsHolder.resolve(args.drop(1) + args.first())
    }

    @Test
    fun can_resolve_rsync_server_send_command() {
        val args = RsyncServerSendCommand().matchArgs
        rsyncCommandsHolder.resolve(args)
        rsyncCommandsHolder.resolve(args + listOf("e.GaAsd"))
        rsyncCommandsHolder.resolve(args + listOf("--something-else"))
        rsyncCommandsHolder.resolve(args + listOf("aaa", "--bb", "foo", "Bar"))
    }
}
