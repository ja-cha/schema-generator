name := "schema-generator"

version := "3.2.0-POSTGRES-SNAPSHOT_1"

organization := "com.abt"

scalaVersion := "2.12.8"

lazy val configPath = TaskKey[Unit]("configPath", "Set path for application.conf")

libraryDependencies ++= List(

  "com.typesafe.play" %% "play-slick" % "5.1.0",
  "com.typesafe.slick" %% "slick" % "3.4.1",
  "com.typesafe.slick" %% "slick-codegen" % "3.4.1",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.4.1",

  "com.fasterxml.jackson.core" % "jackson-annotations" % "2.5.4",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % "2.5.4",
  "com.fasterxml.jackson.core" % "jackson-core" % "2.5.4",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.5.4",

  "com.oracle.database.jdbc" % "ojdbc8" % "12.2.0.1",
  "mysql" % "mysql-connector-java" % "5.1.35",

  "joda-time" % "joda-time" % "2.8.1",
  "ch.qos.logback" % "logback-classic" % "1.1.7" ,

  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.1" % "test",
  "junit" % "junit" % "4.12",

  "com.zaxxer" % "HikariCP-java6" % "2.3.2",
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41"

)

configPath := Def.task {
  sys.props("config.resource") = "./src/main/resources/application.conf"
}

//publishConfiguration := new PublishConfiguration(Some(Path.userHome / ".ivy2" / ".credentials"), "Ja-Cha Repo", Map.empty, Seq.empty, UpdateLogging.Full, true)
