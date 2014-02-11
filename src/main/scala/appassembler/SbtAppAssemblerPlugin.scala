package appassembler

import sbt._
import Keys._
import Load.BuildStructure
import Def.Initialize

import java.io.File

object SbtAppAssemblerPlugin extends Plugin {

  val App = config("app") extend(Runtime)

  val appAssemble = TaskKey[Directory]("assemble", "Builds the app assembly directory")

  val appOutputDirectory = SettingKey[Directory]("app-output-directory")
  val appAutoIncludeDirs = TaskKey[Seq[Directory]]("app-auto-includes-directories","Files are copied from these directories")

  val appJvmOptions = SettingKey[Seq[String]]("app-jvm-options", "JVM parameters to use in start script")

  val appPrograms = TaskKey[Seq[Program]]("programs", "Programs to generate start scripts for")

  val appLibFilter = SettingKey[Jar ⇒ Boolean]("app-lib-filter", "Filter of dependency jar files")
  val appAdditionalLibs = TaskKey[Seq[Jar]]("app-additional-libs", "Additional dependency jar files")
  val appConfig = TaskKey[AppConfig]("appassembler")  

  lazy val appAssemblerSettings: Seq[Setting[_]] =
    inConfig(App)(Seq(
      appAssemble <<= distTask,
      clean <<= distCleanTask,
      dependencyClasspath <<= (dependencyClasspath in Runtime),
      unmanagedResourceDirectories <<= (unmanagedResourceDirectories in Runtime),
      appOutputDirectory := target.value / "dist",
      appAutoIncludeDirs <<= defaultAutoIncludeDirs,
      appJvmOptions := Nil,
      appPrograms <<= (appPrograms in Compile, discoveredMainClasses in Compile) map { (pgs, classes) =>
        val programs = if (!pgs.isEmpty) pgs else classes.map(mc => Program(mc))
        if (programs.isEmpty) sys.error("No Main classes detected.") else programs
      },
      appLibFilter := (_ => true),
      appAdditionalLibs := Seq.empty[File],
      appConfig <<= (appOutputDirectory, appAutoIncludeDirs, appJvmOptions, appPrograms, appLibFilter, appAdditionalLibs) map AppConfig)) ++
      Seq(appAssemble <<= appAssemble in App)

  private def distTask = (appConfig, packageBin in Compile, dependencyClasspath, streams) map {
      (conf, bin, cp, streams) ⇒ 
        streams.log.info("Creating distribution %s ..." format conf.outputDirectory)
        try {
          DistBuilder.createDirectory(conf, bin, cp)(streams.log)
          streams.log.info("Distribution created.")
          conf.outputDirectory
        } catch {
          case e : Exception => sys.error(e.getMessage)
        }
    }

  private def distCleanTask = (appOutputDirectory, streams) map {
      (outDir, s) ⇒        
          s.log.info("Cleaning " + outDir)
          IO.delete(outDir)
  }

  private def defaultAutoIncludeDirs = (sourceDirectory, target) map { (src, target) => 
    Seq(src / "app", target / "app")
  }
}

