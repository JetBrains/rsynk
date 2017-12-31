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

enum class TransmitFlag(val mask: Int) {
    TOP_DIRECTORY       (0b00000000000001),
    SAME_MODE           (0b00000000000010),
    EXTENDED_FLAGS      (0b00000000000100),
    SAME_USER_ID        (0b00000000001000),
    SAME_GROUP_ID       (0b00000000010000),
    SAME_NAME           (0b00000000100000),
    SAME_LONG_NAME      (0b00000001000000),
    SAME_LAST_MODIFIED  (0b00000010000000),
    SAME_RDEV_MAJOR     (0b00000100000000),
    NO_CONTENT_DIRS     (0b00000100000000),
    HARD_LINKED         (0b00001000000000),
    USER_NAME_FOLLOWS   (0b00010000000000),
    GROUP_NAME_FOLLOWS  (0b00100000000000),
    HARD_LINKS_FIRST    (0b01000000000000),
    IO_ERROR_END_LIST   (0b01000000000000),
    MOD_NSEC            (0b10000000000000)
}
