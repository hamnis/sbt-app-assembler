name := "basic"

mainClass := Some("app.Main")

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
    "com.typesafe" %% "scalalogging-slf4j" % "1.1.0",
    "org.slf4j" % "slf4j-api" % "1.7.7",
    "org.slf4j" % "slf4j-log4j12" % "1.7.7",
    "net.hamnaberg" %% "hbase-scala" % "0.4.0",
    "org.apache.spark" %% "spark-core" % "1.0.0",
    "org.constretto" %% "constretto-scala" % "1.0"
)

appAssemblerSettings
