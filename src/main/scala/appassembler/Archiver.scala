package appassembler

import java.io.File
import sbt._
import org.apache.commons.compress.archivers.zip.{ZipArchiveEntry, ZipArchiveOutputStream}

object Archiver {

  def buildFilesystem(root: File) = {
    def entries(f: File): List[File] = f :: (if (f.isDirectory) IO.listFiles(f).toList.flatMap(entries) else Nil)
    def tupled(root: File) = (f: File) => f.getAbsolutePath.substring(root.toString.length).drop(1) -> f
    
    entries(root).tail.map(tupled(root)).toMap    
  }

  def buildPermissions(filesystem: Map[String, File], permissions: Map[String, Int]) = {
    permissions.flatMap { case (path, perm) => 
      val actualKey = {
        val noStar = if (path.endsWith("*")) path.dropRight(1) else path
        if (noStar.startsWith("/")) noStar.drop(1) else noStar
      }
      filesystem.flatMap {case (path2, _) => if (path2.startsWith(actualKey)) Map(path2 -> perm) else Map.empty[String, Int]}
    }.toMap
  }

  def makeZip(fileMap: Map[String, File], permissionMap: Map[String, Int], zipFile: File, logger: Logger) = {
    val containerDirectory = zipFile.getAbsoluteFile.getParentFile
    if (!containerDirectory.exists) {
      IO.createDirectory(containerDirectory)
    }

    val entries = fileMap.map { case (path, f) =>
      val e = new ZipArchiveEntry(f, path)
      permissionMap.get(path).foreach(p => e.setUnixMode(p))
      (f, e)
    }

    val os = new ZipArchiveOutputStream(zipFile)
    
    entries.foreach{case (f, e) =>
      os.putArchiveEntry(e)
      if (f.isFile) {
        IO.transfer(f, os)
      }
      os.closeArchiveEntry()
    }
    os.close()

    logger.info("Wrote " + zipFile)
    zipFile
  }
}
