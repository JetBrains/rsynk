/**
 * Copyright 2016 - 2017 JetBrains s.r.o.
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
package jetbrains.rsynk.server

import jetbrains.rsynk.exitvalues.ArgsParingException
import jetbrains.rsynk.request.RsyncRequest
import java.util.*


internal object RsyncRequestParser {

    private enum class RsyncArgumentKind { rsync, option, file }

    fun parse(args: List<String>): RsyncRequest {

        val files = ArrayList<String>()

        var argKind = RsyncArgumentKind.rsync

        args.forEach action@ { arg ->
            when (argKind) {

                RsyncArgumentKind.rsync -> {
                    if (arg != "rsync") {
                        throw ArgsParingException("'rsync' argument must be first, but was: '$arg'")
                    }
                    argKind = RsyncArgumentKind.option
                }

                RsyncArgumentKind.option -> {
                    if (arg.isShortOption() || arg.isLongOption()) {
                        return@action
                    }
                    if (arg != ".") {
                        throw ArgsParingException("'.' argument expected after options list, got $arg")
                    }
                    argKind = RsyncArgumentKind.file
                }

                RsyncArgumentKind.file -> {
                    files.add(arg)
                }
            }
        }
        return RsyncRequest(files)
    }

    private fun String.isShortOption(): Boolean {
        return length > 1 && startsWith("-")
    }

    private fun String.isLongOption(): Boolean {
        return length > 2 && startsWith("--")
    }
}
