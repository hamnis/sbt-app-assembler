package appassembler

import java.io.File

case class AppConfig (outputDirectory: Directory,
                      autoIncludeDirs: Seq[Directory],
                      distJvmOptions: Seq[String],
                      programs: Seq[Program],
                      libFilter: File ⇒ Boolean,
                      additionalLibs: Seq[File])



case class Program(name: String, mainClass: String)

object Program {
  def apply(mainClass: String) = {
    val idx = mainClass.lastIndexOf('.')
    val name = if (idx == -1) mainClass else mainClass.substring(idx + 1)
    new Program(name.toLowerCase, mainClass)
  }
}

sealed trait Platform
case object Unix extends Platform
case object Windows extends Platform

