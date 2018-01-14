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
package jetbrains.rsynk.rsync.flags

internal enum class ItemFlag(val mask: Int) {
    UP_TO_DATE          (0b00000000),
    REPORT_ATIME        (0b00000001),
    REPORT_CHANGE       (0b00000010),
    REPORT_SIZE         (0b00000100),    /* regular files only */
    REPORT_TIME_FAIL    (0b00000100),    /* symlinks only */
    REPORT_TIME         (0b00001000),
    REPORT_PERMISSIONS  (0b00010000),
    REPORT_OWNER        (0b00100000),
    REPORT_GROUP        (0b01000000),
    REPORT_ACL          (0b10000000),

    REPORT_XATTR        (0b0000000100000000),
    BASIC_TYPE_FOLLOWS  (0b0000100000000000),
    XNAME_FOLLOWS       (0b0001000000000000),
    IS_NEW              (0b0010000000000000),
    LOCAL_CHANGE        (0b0100000000000000),
    TRANSFER            (0b1000000000000000)
}

internal object ItemFlagsValidator {
    fun isFlagSupported(@Suppress("UNUSED_PARAMETER") flags: Int): Boolean = true
}
