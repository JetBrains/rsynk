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
package jetbrains.rsynk.application

import jetbrains.rsynk.files.RsynkFile
import org.apache.sshd.common.keyprovider.KeyPairProvider

class RsynkBuilder internal constructor(var port: Int,
                                        var nioWorkers: Int,
                                        var commandWorkers: Int,
                                        var idleConnectionTimeout: Int,
                                        var serverKeysProvider: KeyPairProvider,
                                        var maxAuthAttempts: Int,
                                        internal val files: List<RsynkFile>) {
    companion object {
        internal val default = RsynkBuilder(
                port = 22,
                nioWorkers = 1,
                commandWorkers = 1,
                idleConnectionTimeout = 50 * 1000,
                serverKeysProvider = KeyPairProvider { emptyList() },
                maxAuthAttempts = 2,
                files = emptyList())
    }

    fun build() = Rsynk(this)
}
