package appassembler

import sbt._
import scala.util.Try
import scala.util.control.Exception._
import classpath.ClasspathUtilities
import java.io.File

object DistBuilder {
  def createDirectory(conf: AppConfig, bin: Jar, classpath: Def.Classpath)(implicit logger: Logger) {
    logger.info("Preparing directory: " + conf.outputDirectory)
    val distBinPath = conf.outputDirectory / "bin"
    val distLibPath = conf.outputDirectory / "lib"
       
    IO.createDirectory(conf.outputDirectory)
    val scripts = new Scripts(conf.distJvmOptions.mkString("", " ", ""), conf.programs)
    scripts.writeScripts(distBinPath, Seq(Unix, Windows))
    copyDirectories(conf.autoIncludeDirs, conf.outputDirectory)        
    copyFiles(Seq(bin), distLibPath)
    copyFiles(libFiles(classpath, conf.libFilter), distLibPath)
    copyFiles(conf.additionalLibs, distLibPath)
    logger.info("Done with directory: " + conf.outputDirectory)
  }

  def packageZip(conf: AppConfig, bin: Jar, classpath: Def.Classpath)(implicit logger: Logger): File = {
    IO.delete(conf.outputDirectory)
    createDirectory(conf, bin, classpath)
    
    val zip = new File(conf.outputDirectory.getParent, conf.outputDirectory.getName + ".zip")
    val fs = Archiver.buildFilesystem(conf.outputDirectory)
    val permissions = Archiver.buildPermissions(fs, Map("/bin/*" -> java.lang.Integer.decode("0755")))
    
    Archiver.makeZip(fs, permissions, zip, logger)
  }

  private def copyDirectories(fromDirs: Seq[Directory], to: Directory) = {
    IO.createDirectory(to)
    for (from ← fromDirs) {
      IO.copyDirectory(from, to)
    }
  }

  private def copyFiles(files: Seq[File], toDir: Directory) = {
    for (f ← files) {
      IO.copyFile(f, new File(toDir, f.getName))
    }
  }

  private def libFiles(classpath: Def.Classpath, libFilter: File ⇒ Boolean): Seq[File] = {
    val libs = classpath.map(_.data).filter(ClasspathUtilities.isArchive)
    libs.map(_.asFile).filter(libFilter)
  }
}
