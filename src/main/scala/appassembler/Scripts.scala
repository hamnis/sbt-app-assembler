package appassembler

import sbt._

class Scripts(jvmOptions: String, programs: Seq[Program]) {

  def writeScripts(to: Directory, platforms: Seq[Platform] = Seq(Unix))(implicit logger: Logger): Unit = programs.foreach{ p =>
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
    |APP_BASE=`dirname "$PRG"`/..
    |
    |# make it fully qualified
    |APP_BASE=`cd "$APP_BASE" && pwd`    
    |
    |if [ "Z${APP_HOME}" = "Z" ]; then
    |  APP_HOME=$APP_BASE
    |fi
    |
    |APP_CLASSPATH="$APP_BASE/lib/*"
    |JAVA_OPTS="$JAVA_OPTS @@jvmOptions@@"
    |
    |exec java $JAVA_OPTS -cp "$APP_CLASSPATH" -Dapp.base="$APP_BASE" -Dapp.home="$APP_HOME" @@mainClass@@ $@
    |""".stripMargin.replace("@@jvmOptions@@", jvmOptions).replace("@@mainClass@@", mainClass)
  }

  private val distBatScript = (jvmOptions: String, mainClass: String) => {
    """|@echo off
    |set APP_HOME=%%~dp0..
    |set APP_CLASSPATH=%%APP_HOME%%\lib\*
    |set JAVA_OPTS=%%JAVA_OPTS%% @@jvmOptions@@
    |set CMD_LINE_ARGS=
    |:setArgs
    |if %1"=="" goto doneSetArgs
    |  set CMD_LINE_ARGS="%%CMD_LINE_ARGS%% %1"
    |  shift
    |  goto setArgs
    |:doneSetArgs
    |
    |java %%JAVA_OPTS%% -cp "%%APP_CLASSPATH%%" -Dapp.base="%%APP_HOME%%" -Dapp.home="%%APP_HOME%%"  @@mainClass@@ "%%CMD_LINE_ARGS%%"
    |""".stripMargin.replace("@@jvmOptions@@", jvmOptions).replace("@@mainClass@@", mainClass)
  }
}
