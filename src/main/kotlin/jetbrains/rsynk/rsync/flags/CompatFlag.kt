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
package jetbrains.rsynk.rsync.flags

enum class CompatFlags(val mask: Byte) {
    INC_RECURSE                     (0b000001),
    SYMLINK_TIMES                   (0b000010),
    SYMLINK_ICONV                   (0b000100),
    SAFE_FILE_LIST                  (0b001000),
    AVOID_FILE_ATTRS_OPTIMIZATION   (0b010000),
    FIXED_CHECKSUM_SEED             (0b100000)
}
