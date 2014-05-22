package appassembler

import java.io.File

case class AppConfig (output: File,
                      autoIncludeDirs: Seq[Directory],
                      distJvmOptions: Seq[String],
                      programs: Seq[Program])



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

