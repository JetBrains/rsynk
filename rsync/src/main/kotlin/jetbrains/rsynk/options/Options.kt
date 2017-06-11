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
package jetbrains.rsynk.options

sealed class Option {
    object Server : Option()
    object Sender : Option()
    object Daemon : Option()

    object Compress : Option()
    class ChecksumSeed(val seed: Int) : Option()
    object ChecksumSeedOrderFix : Option()
    object Delete : Option()

    object FListIOErrorSafety : Option()
    object RelativePaths : Option()
    object ShellCommand : Option()
    object SymlinkTimeSetting : Option()
    object NumericIds : Option()
    object OneFileSystem : Option()
    class PreReleaseInfo(val info: String) : Option()
    object PreserveDevices : Option()
    object PreserveGroup : Option()
    object PreserveLinks : Option()
    object PreserveSpecials : Option()
    object PreserveUser : Option()
    object PreserveXattrs: Option()
    object ProtectArgs : Option()
    object PruneEmptyDirectories : Option()

    sealed class FileSelection : Option() {
        /**
         * Transfer client's file list exactly but exclude directories
         * */
        object NoDirectories : FileSelection()

        /**
         * Transfer client's file list exactly and include directories
         * recursively
         * */
        object Recurse : FileSelection()

        /**
         * Transfer client's file list and the content of any dot directory
         * */
        object TransferDirectoriesWithoutContent : FileSelection()
    }

    object VerboseMode : Option()
}






