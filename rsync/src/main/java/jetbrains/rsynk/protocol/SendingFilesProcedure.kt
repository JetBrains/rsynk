package jetbrains.rsynk.protocol

import jetbrains.rsynk.files.FileListStructure
import jetbrains.rsynk.files.Module

class SendingFilesProcedure(module: Module,
                            options: Set<Option>,
                            checksumSeed: Int,
                            protocolVersion: Int,
                            files: List<String>) {
  val filesList: String = ""
  val files: String = ""

  init {
    if (options.contains(Option.INC_RECURSE)) {
      val flist = FileListStructure(files = emptyList(),
                                    low = 0,
                                    high = 0,
                                    index = 1,
                                    parentDirectoryIndex = 0)
    } else {
      // ndx_start = 0
      // flist_num = 0
    }
  }

  private fun buildFilesList(options: Set<Option>, protocolVersion: Int) {
    if(protocolVersion > 30) {
      val implied_dirs = true
    }
  }
}