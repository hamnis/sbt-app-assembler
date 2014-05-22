val defaultSettings = Seq(
  version := "0.1",
  scalaVersion := "2.10.4"
)


lazy val root =
        project.in( file(".") ).settings(
          name := "multi-parent",
          publish := (),
          publishLocal := (),
          publishArtifact := false
        )
   .aggregate(library, main)

lazy val library = project.settings(defaultSettings : _*).settings(
  libraryDependencies += "io.argonaut" %% "argonaut" % "6.0.4"
)

lazy val main = project.settings(defaultSettings : _*).settings(appAssemblerSettings : _*).settings(
  libraryDependencies += "net.databinder" %% "unfiltered-netty-server" % "0.8.0"
).dependsOn(library)

