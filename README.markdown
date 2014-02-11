## What is sbt-app-assembler
Its a sbt plugin to assemble the application and generate start up scripts

Fork of https://github.com/jestan/sbt-app-assembler

Restructure and cleanup of the plugin.

## Requirements
 sbt 0.13.x

## Installation

Add the following lines to PROJECT_DIR/project/plugin.sbt

```scala
addSbtPlugin("net.hamnaberg.sbt" % "sbt-appassembler" % "0.2.0")
```

By default the plugin will detect all main classes and generate a script for it.

Inject plugin settings into project in build.sbt:

```scala
appAssemblerSettings

appAssemblerJvmOptions := Seq(
  "-Xms1024M", 
  "-Xmx1024M",
  "-Xss1M",
  "-XX:MaxPermSize=256M",
  "-XX:+UseParallelGC"
)
```
## Usage

  Use 'assemble' sbt task to create the application assembly

## Overriding the programs generated

```scala
appPrograms := Seq(appassembly.Program("server", "com.example.Main"))
```
