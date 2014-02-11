package appassembler

import sbt._

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

class Scripts(jvmOptions: String, programs: Seq[Program]) {

  def writeScripts(to: Directory, platforms: Seq[Platform] = Seq(Unix))(implicit logger: Logger): Unit = programs.foreach { p =>
    logger.info("Generating program '%s' for mainClass '%s'" format(p.name, p.mainClass))
    platforms.foreach(writeScript(to, p, _))
  }

  def writeScript(to: Directory, program: Program, platform: Platform) = {
    val (target, script) = platform match {
      case Unix => (new File(to, program.name), distShScript)
      case Windows => (new File(to, program.name + ".bat"), distBatScript)
    }        
    IO.write(target, script(jvmOptions, program.mainClass))
    target.setExecutable(true, false)
  }

  private val distShScript = (jvmOptions: String, mainClass: String) => {
    """|#!/bin/sh
    |
    |APP_HOME="$(cd "$(cd "$(dirname "$0")"; pwd -P)"/..; pwd)"
    |APP_CLASSPATH="$APP_HOME/lib/*:$APP_HOME/conf"
    |JAVA_OPTS="%s"
    |
    |java $JAVA_OPTS -cp "$APP_CLASSPATH" -Dapp.home="$APP_HOME" %s
    |""".stripMargin.format(jvmOptions, mainClass)
  }

  private val distBatScript = (jvmOptions: String, mainClass: String) => {
    """|@echo off
    |set APP_HOME=%%~dp0..
    |set APP_CLASSPATH=%%APP_HOME%%\lib\*;%%APP_HOME%%\conf
    |set JAVA_OPTS=%s
    |
    |java %%JAVA_OPTS%% -cp "%%APP_CLASSPATH%%" -Dapp.home="%%APP_HOME%%" %s
    |""".stripMargin.format(jvmOptions, mainClass)
  }
}
