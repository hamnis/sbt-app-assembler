package appassembler

import sbt._
import scala.util.Try
import scala.util.control.Exception._
import classpath.ClasspathUtilities

object DistBuilder {
  def create(conf: AppConfig, bin: Jar, classpath: Def.Classpath)(implicit logger: Logger): Unit = {
    val distBinPath = conf.outputDirectory / "bin"
    val distConfigPath = conf.outputDirectory / "conf"
    val distLibPath = conf.outputDirectory / "lib"
       
    IO.createDirectory(conf.outputDirectory)
    val scripts = new Scripts(conf.distJvmOptions.mkString("", " ", ""), conf.programs)
    scripts.writeScripts(distBinPath, Seq(Unix, Windows))
    copyDirectories(conf.configSourceDirs, distConfigPath)        
    copyFiles(Seq(bin), distLibPath)
    copyFiles(libFiles(classpath, conf.libFilter), distLibPath)
    copyFiles(conf.additionalLibs, distLibPath)
    ()
  }

  private def copyDirectories(fromDirs: Seq[File], to: File) = {
    IO.createDirectory(to)
    for (from ← fromDirs) {
      IO.copyDirectory(from, to)
    }
  }

  private def copyFiles(files: Seq[File], toDir: File) = {
    for (f ← files) {
      IO.copyFile(f, new File(toDir, f.getName))
    }
  }

  private def libFiles(classpath: Def.Classpath, libFilter: File ⇒ Boolean): Seq[File] = {
    val libs = classpath.map(_.data).filter(ClasspathUtilities.isArchive)
    libs.map(_.asFile).filter(libFilter)
  }
}
