import sbt._
import Keys._

object versions {
    val akkaVersion = "2.3.9"
}

object MyBuild extends Build {
  lazy val LeakyJournal = Project(
    id = "LeakyJournal",
    base = file("."),
    settings = Defaults.defaultSettings ++ Seq(
      scalaVersion := "2.11.5",
      fork         := true,
      libraryDependencies ++= Seq(
        "com.typesafe.akka"   %% "akka-actor"                    % versions.akkaVersion,
        "com.typesafe.akka"   %% "akka-persistence-experimental" % versions.akkaVersion
      )
    )
  )
}
