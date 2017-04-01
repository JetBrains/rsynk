package jetbrains.rsynk.server

import jetbrains.rsynk.command.AllCommandsResolver
import jetbrains.rsynk.command.CommandNotFoundException
import jetbrains.rsynk.files.FileInfoReader
import jetbrains.rsynk.files.UnixDefaultFileSystemInfo
import org.junit.Test

class CommandsResolverTest {

    private val rsyncCommandsHolder = AllCommandsResolver(FileInfoReader(UnixDefaultFileSystemInfo()))

    @Test(expected = CommandNotFoundException::class)
    fun empty_args_list_does_not_match_any_command_test() {
        rsyncCommandsHolder.resolve(emptyList())
    }

    @Test
    fun can_resolve_rsync_server_send_command_test() {
        val args = listOf("rsync", "--server", "--sender")
        rsyncCommandsHolder.resolve(args)
    }

    @Test(expected = CommandNotFoundException::class)
    fun do_not_resolve_server_send_command_for_client_in_daemon_mode_test() {
        val args = listOf("rsync", "--server", "--sender", "--daemon")
        rsyncCommandsHolder.resolve(args)
    }
}
