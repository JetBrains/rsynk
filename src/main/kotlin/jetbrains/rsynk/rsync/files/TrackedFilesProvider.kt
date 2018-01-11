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

internal interface TrackedFilesProvider {
    fun resolve(path: String): RsynkFile? = resolve(listOf(path)).values.firstOrNull()

    /**
     * Return file paths mapped to corresponding
     * tracked files only if all requested files are tracked
     * by rsynk.
     * If at least one file is not tracked by rsynk - throw
     * [jetbrains.rsynk.rsync.exitvalues.InvalidFileException]
     *
     * [paths] are absolute paths requested by the client.
     */
    fun resolve(paths: List<String>): Map<String, RsynkFile>
}
