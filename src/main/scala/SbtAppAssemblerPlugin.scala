
import sbt._
import Keys._
import Load.BuildStructure
import classpath.ClasspathUtilities
import Def.Initialize

import java.io.File

object SbtAppAssemblerPlugin extends Plugin {

  case class DistConfig(outputDirectory: File,
                        configSourceDirs: Seq[File],
                        distJvmOptions: Seq[String],
                        distMainClass: String,
                        libFilter: File ⇒ Boolean,
                        additionalLibs: Seq[File])

  val Assemble = config("app-assembler") extend (Runtime)
  val dist = TaskKey[File]("assemble", "Builds the app assembly directory")
  val distClean = TaskKey[Unit]("clean", "Removes the app assembly directory")

  val outputDirectory = SettingKey[File]("app-assembler-output-directory")
  val configSourceDirs = TaskKey[Seq[File]]("app-assembler-conf-source-directories","Configuration files are copied from these directories")

  val appAssemblerJvmOptions = SettingKey[Seq[String]]("app-assembler-jvm-options", "JVM parameters to use in start script")
  val appAssemblerMainClass = TaskKey[String]("app-assembler-main-class", "App main class to use in start script")

  val libFilter = SettingKey[File ⇒ Boolean]("app-assembler-lib-filter", "Filter of dependency jar files")
  val additionalLibs = TaskKey[Seq[File]]("app-assembleradditional-libs", "Additional dependency jar files")
  val distConfig = TaskKey[DistConfig]("app-assembler")

  lazy val appAssemblerSettings: Seq[Setting[_]] =
    inConfig(Assemble)(Seq(
      dist <<= distTask,
      distClean <<= distCleanTask,
      dependencyClasspath <<= (dependencyClasspath in Runtime),
      unmanagedResourceDirectories <<= (unmanagedResourceDirectories in Runtime),
      outputDirectory := target.value / "dist",
      configSourceDirs <<= defaultConfigSourceDirs,
      appAssemblerJvmOptions := Nil,
      appAssemblerMainClass := (mainClass in Compile).value.get,
      libFilter := {
        f ⇒ true
      },
      additionalLibs <<= defaultAdditionalLibs,
      distConfig <<= (outputDirectory, configSourceDirs, appAssemblerJvmOptions, appAssemblerMainClass, libFilter, additionalLibs) map DistConfig)) ++
      Seq(dist <<= dist in Assemble)

  private def distTask: Initialize[Task[File]] =
    (distConfig, sourceDirectory, packageBin in Compile, dependencyClasspath, streams) map {
      (conf, src, bin, cp, streams) ⇒

        val distBinPath = conf.outputDirectory / "bin"
        val distConfigPath = conf.outputDirectory / "conf"
        val distLibPath = conf.outputDirectory / "lib"
        
        streams.log.info("Creating distribution %s ..." format conf.outputDirectory)
        IO.createDirectory(conf.outputDirectory)
        Scripts(conf.distJvmOptions.mkString("", " ", ""), conf.distMainClass).writeScripts(distBinPath)
        copyDirectories(conf.configSourceDirs, distConfigPath)        
        copyFiles(Seq(bin), distLibPath)
        copyFiles(libFiles(cp, conf.libFilter), distLibPath)
        copyFiles(conf.additionalLibs, distLibPath)

        streams.log.info("Distribution created.")

        conf.outputDirectory
    }

  private def distCleanTask: Initialize[Task[Unit]] =
    (outputDirectory, streams) map {
      (outDir, s) ⇒        
          val log = s.log
          log.info("Cleaning " + outDir)
          IO.delete(outDir)
  }

  private def defaultConfigSourceDirs = (sourceDirectory, unmanagedResourceDirectories) map {
    (src, resources) ⇒
      Seq(src / "conf", src / "main" / "conf") ++ resources
  }

  private def defaultAdditionalLibs = (libraryDependencies) map {
    (libs) ⇒
      Seq.empty[File]
  }

  private case class Scripts(jvmOptions: String, mainClass: String) {

    def writeScripts(to: File) = {
      scripts.map {
        script ⇒
          val target = new File(to, script.name)
          IO.write(target, script.contents)
          setExecutable(target, script.executable)
      }.foldLeft(None: Option[String])(_ orElse _)
    }

    private case class DistScript(name: String, contents: String, executable: Boolean)

    private def scripts = Set(DistScript("start", distShScript, true), DistScript("start.bat", distBatScript, true))

    private def distShScript =
      """|#!/bin/sh
    |
    |APP_HOME="$(cd "$(cd "$(dirname "$0")"; pwd -P)"/..; pwd)"
    |APP_CLASSPATH="$APP_HOME/lib/*:$APP_HOME/conf"
    |JAVA_OPTS="%s"
    |
    |java $JAVA_OPTS -cp "$APP_CLASSPATH" -Dapp.home="$APP_HOME" %s
    |""".stripMargin.format(jvmOptions, mainClass)

    private def distBatScript =
      """|@echo off
    |set APP_HOME=%%~dp0..
    |set APP_CLASSPATH=%%APP_HOME%%\lib\*;%%APP_HOME%%\conf
    |set JAVA_OPTS=%s
    |
    |java %%JAVA_OPTS%% -cp "%%APP_CLASSPATH%%" -Dapp.home="%%APP_HOME%%" %s
    |""".stripMargin.format(jvmOptions, mainClass)

    private def setExecutable(target: File, executable: Boolean): Option[String] = {
      val success = target.setExecutable(executable, false)
      if (success) None else Some("Couldn't set permissions of " + target)
    }
  }

  private def copyDirectories(fromDirs: Seq[File], to: File) = {
    IO.createDirectory(to)
    for (from ← fromDirs) {
      IO.copyDirectory(from, to)
    }
  }

  private def copyJars(fromDir: File, toDir: File) = {
    val jarFiles = fromDir.listFiles.filter(f ⇒
      f.isFile &&
        f.name.endsWith(".jar") &&
        !f.name.contains("-sources") &&
        !f.name.contains("-docs"))

    copyFiles(jarFiles, toDir)
  }

  private def copyFiles(files: Seq[File], toDir: File) = {
    for (f ← files) {
      IO.copyFile(f, new File(toDir, f.getName))
    }
  }

  private def libFiles(classpath: Classpath, libFilter: File ⇒ Boolean): Seq[File] = {
    val (libs, directories) = classpath.map(_.data).partition(ClasspathUtilities.isArchive)
    libs.map(_.asFile).filter(libFilter)
  }
}

