
sbtPlugin := true

organization := "net.hamnaberg.sbt"

name := "sbt-appassembler"

version := "0.2.0-SNAPSHOT"

publishMavenStyle := true

scalacOptions := Seq("-deprecation")

ScriptedPlugin.scriptedSettings

credentials += Credentials(Path.userHome / ".sbt" / ".credentials")

publishTo <<= (version) { version: String =>
  if (version.trim.endsWith("SNAPSHOT"))
    Some("Repository Archiva Managed snapshots Repository" at "https://oss.sonatype.org/content/repositories/snapshots")
  else
    Some("Repository Archiva Managed internal Repository" at "https://oss.sonatype.org/service/local/staging/deploy/maven2")
}


