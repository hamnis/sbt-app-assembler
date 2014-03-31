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
    |PRG="$0"
    |
    |# need this for relative symlinks
    |while [ -h "$PRG" ] ; do
    |  ls=`ls -ld "$PRG"`
    |  link=`expr "$ls" : '.*-> \(.*\)$'`
    |  if expr "$link" : '/.*' > /dev/null; then
    |    PRG="$link"
    |  else
    |    PRG="`dirname "$PRG"`/$link"
    |  fi
    |done
    |
    |APP_HOME=`dirname "$PRG"`/..
    |
    |# make it fully qualified
    |APP_HOME=`cd "$APP_HOME" && pwd`
    |
    |APP_CLASSPATH="$APP_HOME/lib/*"
    |JAVA_OPTS="%s"
    |
    |java $JAVA_OPTS -cp "$APP_CLASSPATH" -Dapp.home="$APP_HOME" %s
    |""".stripMargin.format(jvmOptions, mainClass)
  }

  private val distBatScript = (jvmOptions: String, mainClass: String) => {
    """|@echo off
    |set APP_HOME=%%~dp0..
    |set APP_CLASSPATH=%%APP_HOME%%\lib\*
    |set JAVA_OPTS=%s
    |
    |java %%JAVA_OPTS%% -cp "%%APP_CLASSPATH%%" -Dapp.home="%%APP_HOME%%" %s
    |""".stripMargin.format(jvmOptions, mainClass)
  }
}
