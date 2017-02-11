package jetbrains.rsynk.server

import jetbrains.rsynk.command.CommandNotFoundException
import jetbrains.rsynk.command.RsyncCommandsHolder
import jetbrains.rsynk.command.RsyncServerSendCommand
import org.junit.Test

class RsyncCommandHolderTest {

  val rsyncCommandsHolder = RsyncCommandsHolder()

  @Test(expected = CommandNotFoundException::class)
  fun empty_args_list_does_not_match_any_command() {
    rsyncCommandsHolder.resolve(emptyList())
  }

  @Test(expected = CommandNotFoundException::class)
  fun rsync_argument_should_be_in_first_place_to_resolve_command() {
    val args = RsyncServerSendCommand().args
    rsyncCommandsHolder.resolve(args.drop(1) + args.first())
  }

  @Test
  fun can_resolve_rsync_server_send_command() {
    val args = RsyncServerSendCommand().args
    rsyncCommandsHolder.resolve(args)
    rsyncCommandsHolder.resolve(args + listOf("e.GaAsd"))
    rsyncCommandsHolder.resolve(args + listOf("--something-else"))
    rsyncCommandsHolder.resolve(args + listOf("aaa", "--bb", "foo", "Bar"))
  }

  @Test
  fun can_resolve_rsync_server_send_command_reordered_args() {
    val args = RsyncServerSendCommand().args
    val rsync = args.first()
    val rest = args.drop(1)

    rsyncCommandsHolder.resolve(listOf(rsync) + rest.sorted())
    rsyncCommandsHolder.resolve(listOf(rsync) + rest.sortedDescending())
    rsyncCommandsHolder.resolve(listOf(rsync) + rest.reversed())

    rsyncCommandsHolder.resolve(listOf(rsync) + listOf("--something-else") + rest.sorted())
    rsyncCommandsHolder.resolve(listOf(rsync) + listOf("--something-else") + rest.sortedDescending())
    rsyncCommandsHolder.resolve(listOf(rsync) + listOf("--something-else") + rest.reversed())

    rsyncCommandsHolder.resolve(listOf(rsync) + (listOf("aaa", "zz", "--bb", "foo", "BaR") + rest).sorted())
    rsyncCommandsHolder.resolve(listOf(rsync) + (listOf("aaa", "zz", "--bb", "foo", "BaR") + rest).sortedDescending())
    rsyncCommandsHolder.resolve(listOf(rsync) + (listOf("aaa", "zz", "--bb", "foo", "BaR") + rest).reversed())
  }
}
