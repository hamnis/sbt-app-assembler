package appassembler

import sbt._
import Keys._
import Load.BuildStructure
import Def.Initialize

import java.io.File

object SbtAppAssemblerPlugin extends Plugin {

  val App = config("app") extend(Runtime)

  val appAssemble = TaskKey[Directory]("assemble", "Builds the app assembly directory")

  val appOutput = SettingKey[File]("Output, May be anything that is supported by scala-archiver.")
  val appAutoIncludeDirs = TaskKey[Seq[Directory]]("Files are copied from these directories")

  val appJvmOptions = SettingKey[Seq[String]]("JVM parameters to use in start script")

  val appPrograms = TaskKey[Seq[Program]]("Programs to generate start scripts for")

  val appLibFilter = SettingKey[Jar ⇒ Boolean]("Filter of dependency jar files")
  val appAdditionalLibs = TaskKey[Seq[Jar]]("Additional dependency jar files")
  val appConfig = TaskKey[AppConfig]("Configuration, internally used")  

  lazy val appAssemblerSettings: Seq[Setting[_]] =
    inConfig(App)(Seq(
      appAssemble <<= distTask,
      packageBin <<= distTask,
      clean <<= distCleanTask,
      dependencyClasspath <<= (dependencyClasspath in Runtime),
      unmanagedResourceDirectories <<= (unmanagedResourceDirectories in Runtime),
      appOutput := target.value / "appassembler",
      appAutoIncludeDirs <<= defaultAutoIncludeDirs,
      appJvmOptions := Nil,
      appPrograms <<= (appPrograms in Compile, discoveredMainClasses in Compile) map { (pgs, classes) =>
        val programs = if (!pgs.isEmpty) pgs else classes.map(mc => Program(mc))
        if (programs.isEmpty) sys.error("No Main classes detected.") else programs
      },
      appLibFilter := (_ => true),
      appAdditionalLibs := Seq.empty[File],
      appConfig <<= (appOutput, appAutoIncludeDirs, appJvmOptions, appPrograms, appLibFilter, appAdditionalLibs) map AppConfig)) ++
      Seq(appAssemble <<= appAssemble in App, appPrograms in Compile := Nil)

  private def distTask = (appConfig, packageBin in Compile, dependencyClasspath, streams) map {
      (conf, bin, cp, streams) ⇒ 
        streams.log.info("Creating distribution %s ...".format(conf.output))
        try {
          val file = DistBuilder.create(conf, bin, cp)(streams.log)
          streams.log.info("Distribution created in %s.".format(file))
          file
        } catch {
          case e : Exception => {e.printStackTrace(); sys.error(e.getMessage)}
        }
    }

  private def distCleanTask = (appOutput, streams) map {
      (output, s) ⇒        
          s.log.info("Cleaning " + output)
          IO.delete(output)
  }

  private def defaultAutoIncludeDirs = (sourceDirectory, target) map { (src, target) => 
    Seq(src / "app", target / "app")
  }
}

