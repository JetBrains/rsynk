/**
 * Copyright 2016 - 2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jetbrains.rsynk.rsync.files

import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.PosixFileAttributes

interface FileSystemInfo {
    val defaultUser: User
    val defaultGroup: Group
    val defaultFilePermission: Int
    val defaultDirPermission: Int

    fun getOwner(file: Path): User
    fun getGroup(file: Path): Group
}

class UnixDefaultFileSystemInfo : FileSystemInfo {

    private val nobodyId = Math.pow(2.0, 16.0).toInt() - 1 - 1
    private val umask: Int

    init {
        val umaskText = System.getProperty("umask") ?: "0022"
        umask = umaskText.toInt(8)
    }

    override val defaultUser: User = User("nobody", nobodyId)

    override val defaultGroup: Group = Group("nobody", nobodyId)

    override val defaultFilePermission: Int = 666 and umask.inv()

    override val defaultDirPermission: Int = 777 and umask.inv()

    override fun getOwner(file: Path): User {
        val uid = Files.getAttribute(file, "unix:uid", LinkOption.NOFOLLOW_LINKS) as Int
        val userName = Files.readAttributes(file, PosixFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS).owner().name
        return User(userName, uid)
    }

    override fun getGroup(file: Path): Group {
        val gid = Files.getAttribute(file, "unix:gid", LinkOption.NOFOLLOW_LINKS) as Int
        val groupName = Files.readAttributes(file, PosixFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS).group().name
        return Group(groupName, gid)
    }
}

