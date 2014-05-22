package appassembler

import sbt._
import scala.util.Try
import scala.util.control.Exception._
import classpath.ClasspathUtilities
import java.io.File
import archiver.{FileMapping, Archiver, FilePermissions, Packaging}

object DistBuilder {
  val permissions = Map("/bin/*" -> FilePermissions(Integer.decode("0755")).get)

  def create(conf: AppConfig, classpath: Seq[File])(implicit logger: Logger): File = {
    if (conf.output.exists) {
      archiver.IO.delete(conf.output) //TODO: Should this really be required?
    }

    IO.withTemporaryDirectory{ temp => 
      val distBinPath = temp / "bin"
      val scripts = new Scripts(conf.distJvmOptions.mkString("", " ", ""), conf.programs)
      scripts.writeScripts(distBinPath, Seq(Unix, Windows)) 
      
      val mapping = {
        val auto = FileMapping(conf.autoIncludeDirs.toList)
        val binary = FileMapping(List(distBinPath), base = Some("bin"), permissions = permissions)
        val libraries = classpath
        val mapping = libraries.foldLeft(Map.empty[String, File]){case (m, f) => m.updated(("lib/" + f.getName), f)}
        FileMapping(mapping, permissions).append(auto).append(binary)
      }

      val archiver = Archiver(Packaging(conf.output))
      archiver.create(mapping, conf.output)
    }
  }
}
