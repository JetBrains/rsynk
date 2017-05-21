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
package jetbrains.rsynk.files

import java.nio.file.Files
import java.nio.file.Path

class FilterList {

    //TODO
    fun include(file: Path): Boolean {
        if (Files.isDirectory(file)) {
            throw IllegalArgumentException("Use 'includeDir' method for directories")
        }
        return true
    }

    //TODO
    fun includeDir(dir: Path): Boolean {
        if (!Files.isDirectory(dir)) {
            throw IllegalArgumentException("Use 'include' method for files")
        }
        return true
    }
}
