
sbtPlugin := true

organization := "net.hamnaberg.sbt"

name := "sbt-appassembler"

publishMavenStyle := true

scalacOptions := Seq("-deprecation")

ScriptedPlugin.scriptedSettings

libraryDependencies += "net.hamnaberg" %% "scala-archiver" % "0.2.0"

resolvers += Opts.resolver.sonatypeSnapshots
