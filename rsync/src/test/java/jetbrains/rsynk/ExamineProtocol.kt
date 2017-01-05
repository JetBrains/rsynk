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
    /usr/local/bin/rsync --server --daemon --port=10023
 rsyncd.conf=
    lock file = /usr/local/var/rsync.lock
    log file = /usr/local/var/log/rsyncd.log
    pid file = /usr/local/var/run/rsyncd.pid

    [sanbox]
        path = /Users/jetbrains/Desktop/sandbox
        comment = The documents folder of Juan
        uid = jetbrains
        gid = jetbrains
        read only = no
        list = yes
        auth users = rsyncclient
        secrets file = /usr/local/etc/rsyncd.secrets
*/

fun main(args: Array<String>) {
  val list = ArrayList<String>()
  Socket("localhost", 10023).use { socket ->

    // > write version
    socket.outputStream.write("@RSYNCD: 31.0\n".toByteArray())
    socket.outputStream.flush()

    // < read version
    val version = read(socket.inputStream)
    list.add(String(version))

    // > write desired module
    socket.outputStream.write("\n".toByteArray())

    // < authentication request
    val authRequest = read(socket.inputStream)
    list.add(String(authRequest))

    // > authentication response
    socket.outputStream.write("haha\n".toByteArray())
    socket.outputStream.flush()

    // < ?
    val next = read(socket.inputStream)
    list.add(String(next))
  }

  println(list)
}