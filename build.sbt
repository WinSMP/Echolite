import Dependencies._
import sbtassembly.AssemblyPlugin.defaultShellScript

lazy val buildScalaVersion = "3.3.5"
lazy val pluginVersion = "0.4.2"
lazy val projectName = "Echolite"
lazy val projectPackage = "org.winlogon.echolite"

ThisBuild / scalaVersion     := buildScalaVersion
ThisBuild / version          := s"$pluginVersion-SNAPSHOT"
ThisBuild / organization     := "org.winlogon"
ThisBuild / organizationName := "winlogon"
Compile / mainClass := Some(s"$projectPackage.$projectName")

// GitHub CI
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("21"))
ThisBuild / publishTo := None
publish / skip := true

crossScalaVersions := Seq(buildScalaVersion)

lazy val root = (project in file("."))
  .settings(
    name := projectName,
    assembly / assemblyOption := (assembly / assemblyOption).value.withIncludeScala(false),
  )

// Merge strategy for avoiding conflicts in dependencies
assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}

assembly / mainClass := Some(s"$projectPackage.$projectName")

libraryDependencies ++= Seq(
  "io.papermc.paper" % "paper-api" % "1.21.10-R0.1-SNAPSHOT" % Provided,
  "net.dv8tion" % "JDA" % "6.1.3" % Provided,
  "dev.vankka" % "mcdiscordreserializer" % "4.4.0" % Provided,
  "org.scalatest" %% "scalatest" % "3.2.19" % Test,
)

resolvers += "papermc-repo" at "https://repo.papermc.io/repository/maven-public/"
