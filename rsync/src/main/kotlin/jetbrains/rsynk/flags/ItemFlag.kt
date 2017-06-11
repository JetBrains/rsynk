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
package jetbrains.rsynk.flags

sealed class ItemFlag(val code: Int) {
    object UpToDate : ItemFlag(0)
    object ReportAtime : ItemFlag(1 shl 0)
    object ReportChange : ItemFlag(1 shl 1)
    object ReportSize : ItemFlag(1 shl 2)         /* regular files only */
    object ReportTimeFail : ItemFlag(1 shl 2)     /* symlinks only */
    object ReportTime : ItemFlag(1 shl 3)
    object ReportPermissions : ItemFlag(1 shl 4)
    object ReportOwner : ItemFlag(1 shl 5)
    object ReportGroup : ItemFlag(1 shl 6)
    object ReportACL : ItemFlag(1 shl 7)
    object ReportXATTR : ItemFlag(1 shl 8)
    object BasicTypeFollows : ItemFlag(1 shl 11)
    object XNameFollows : ItemFlag(1 shl 12)
    object IsNew : ItemFlag(1 shl 13)
    object LocalChange : ItemFlag(1 shl 14)
    object Transfer : ItemFlag(1 shl 15)
}

object ItemFlagsValidator {
    fun isFlagSupported(f: Int): Boolean = true
}

fun Char.decodeItemFlags(): Set<ItemFlag> {
    val thisIntValue = this.toInt()
    return listOf(ItemFlag.UpToDate,
            ItemFlag.ReportAtime,
            ItemFlag.ReportChange,
            ItemFlag.ReportSize,
            ItemFlag.ReportTimeFail,
            ItemFlag.ReportTime,
            ItemFlag.ReportPermissions,
            ItemFlag.ReportOwner,
            ItemFlag.ReportGroup,
            ItemFlag.ReportACL,
            ItemFlag.ReportXATTR,
            ItemFlag.BasicTypeFollows,
            ItemFlag.XNameFollows,
            ItemFlag.IsNew,
            ItemFlag.LocalChange,
            ItemFlag.Transfer)
            .filter { flag -> thisIntValue and flag.code != 0 }
            .toSet()
}
