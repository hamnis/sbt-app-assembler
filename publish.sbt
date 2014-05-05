publishTo <<= (version) apply {
  (v: String) => if (v.trim().endsWith("SNAPSHOT")) Some(Opts.resolver.sonatypeSnapshots) else Some(Opts.resolver.sonatypeStaging)
}

credentials += Credentials(Path.userHome / ".sbt" / ".credentials")

homepage := Some(new URL("http://github.com/hamnis/sbt-app-assembler"))

startYear := Some(2014)

licenses := Seq(("Apache 2", new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")))

pomExtra <<= (pomExtra, name, description) {(pom, name, desc) => pom ++ xml.Group(
  <scm>
    <url>http://github.com/hamnis/sbt-app-assembler</url>
    <connection>scm:git:git://github.com/hamnis/sbt-app-assembler.git</connection>
    <developerConnection>scm:git:git@github.com:hamnis/sbt-app-assembler.git</developerConnection>
  </scm>
  <developers>
    <developer>
      <id>hamnis</id>
      <name>Erlend Hamnaberg</name>
      <url>http://twitter.com/hamnis</url>
    </developer>
  </developers>
)}

useGpg := true

aetherPublishSettings

aetherArtifact <<= (coordinates, Keys.`package` in Compile, makePom in Compile, com.typesafe.sbt.pgp.PgpKeys.signedArtifacts in Compile) map {
  (coords: aether.MavenCoordinates, mainArtifact: File, pom: File, artifacts: Map[Artifact, File]) =>
    aether.Aether.createArtifact(artifacts, pom, coords, mainArtifact)
}

com.typesafe.sbt.pgp.PgpKeys.publishSigned <<= deploy
