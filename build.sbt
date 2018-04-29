import com.heroku.sbt.HerokuPlugin.autoImport._
import play.sbt.PlayScala

name := "cvcloudclient"

version := "1.0"

scalaVersion := "2.11.11"

herokuAppName in Compile := "cvcloudclient"

val akka = "2.4.17"
val json4sVersion = "3.4.0"
val mongo = "0.12.1"
val http ="10.0.5"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akka,
  "com.typesafe.akka" %% "akka-http" % http,
  "com.typesafe.akka" %% "akka-http-core" % http,
  "org.json4s" %% "json4s-jackson" % json4sVersion,
  "org.reactivemongo" %% "reactivemongo" % mongo,
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "com.jason-goodwin" %% "authentikat-jwt" % "0.4.5"
)

libraryDependencies += jdbc
libraryDependencies += cache
libraryDependencies += ws
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % Test
libraryDependencies += filters
