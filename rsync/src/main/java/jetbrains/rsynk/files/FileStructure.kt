package jetbrains.rsynk.files

import java.io.File

class FileStructure (val directory: File,    /* The dir info inside the transfer */
                     val lastModified: Long, /* When the item was last modified */
                     val length32: Long,     /* File size % 2 ^ 32 */
                     val mode: Int,          /* The item's type and permissions */
                     val flags: Int,         /* The FLAG_* bits for this item */
                     val file: File)         /* The basename (AKA filename) follows */
