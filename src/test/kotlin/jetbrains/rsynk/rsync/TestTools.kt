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
package jetbrains.rsynk.rsync


internal object OS {
    val isLinux: Boolean
        get() {
            val os = os()
            return os.contains("linux")
        }

    val isMac: Boolean
        get() {
            val os = os()
            return os.contains("mac") || os.contains("os x") || os.contains("darwin")
        }

    val isWindows: Boolean
        get() {
            return os().contains("windows")
        }

    private fun os() = System.getProperty("os.name")?.toLowerCase() ?: ""
}
