package appassembler

import sbt._

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
