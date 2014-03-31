package appassembler

import sbt._
import scala.util.Try
import scala.util.control.Exception._
import classpath.ClasspathUtilities
import java.io.File
import archiver.{FileMapping, Archiver, FilePermissions, Packaging}

object DistBuilder {
  val permissions = Map("/bin/*" -> FilePermissions(Integer.decode("0755")).get)

  def create(conf: AppConfig, bin: Jar, classpath: Def.Classpath)(implicit logger: Logger): File = {
    IO.withTemporaryDirectory{ temp => 
      val distBinPath = temp / "bin"
      val scripts = new Scripts(conf.distJvmOptions.mkString("", " ", ""), conf.programs)
      scripts.writeScripts(distBinPath, Seq(Unix, Windows)) 

      val mapping = {
        val auto = FileMapping(conf.autoIncludeDirs.toList)
        val binary = FileMapping(List(distBinPath), base = Some("bin"))
        val libraries = libFiles(classpath, conf.libFilter) ++ conf.additionalLibs ++ Seq(bin)
        val mapping = libraries.foldLeft(Map.empty[String, File]){case (m, f) => m.updated(("lib/" + f.getName), f)}
        new FileMapping(mapping, permissions).append(auto).append(binary)
      }

      val archiver = Archiver(Packaging(conf.output))
      archiver.create(mapping, conf.output)
    }
  }

  private def libFiles(classpath: Def.Classpath, libFilter: File ⇒ Boolean): Seq[File] = {
    val libs = classpath.map(_.data).filter(ClasspathUtilities.isArchive)
    libs.map(_.asFile).filter(libFilter)
  }
}
