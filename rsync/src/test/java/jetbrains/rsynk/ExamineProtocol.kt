package jetbrains.rsynk

import java.io.InputStream
import java.net.Socket
import java.util.*


/**
 * NOTE:
 * This file helps understand rsync protocol
 * and will be deleted once work is done
 */

fun read(stream: InputStream): ByteArray {
  val available = stream.available()
  if (available == 0) {
    return byteArrayOf()
  }
  val bytes = ByteArray(available)
  val read = stream.read(bytes)
  println("Read $read bytes")
  return bytes
}

/*
 rsync cmd
    sudo rsync --daemon --port=11112

 rsyncd.conf
    lock file = /usr/local/var/rsync.lock
    log file = /usr/local/var/log/rsyncd.log
    pid file = /usr/local/var/run/rsyncd.pid

    [sandbox]
        path = /Users/jetbrains/Desktop/sandbox
        comment = The documents folder of Juan
        uid = jetbrains
        read only = no
        list = yes

    [papers]
        path = /Users/jetbrains/Desktop/Papers
        comment = phd thesises
        uid = jetbrains
        read only = no
        list = yes
*/

fun main(args: Array<String>) {
  val list = ArrayList<String>()
  Socket("localhost", 11112).use { socket ->

    // > write version
    socket.outputStream.write("@RSYNCD: 31.0\n".toByteArray())
    socket.outputStream.flush()

    // < read version
    val version = read(socket.inputStream)
    list.add(String(version))

    // > write desired module
    socket.outputStream.write("sandbox\n".toByteArray())
    socket.outputStream.flush()

    // < authentication
    val authChallenge = String(read(socket.inputStream))
    list.add(authChallenge)

    // > write options
    val whatIsNext = String(read(socket.inputStream))
    list.add(whatIsNext)
  }

  println(list)
}