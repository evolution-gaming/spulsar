import Dependencies._

name := "spulsar"

organization := "com.evolutiongaming"

homepage := Some(new URL("http://github.com/evolution-gaming/spulsar"))

startYear := Some(2019)

organizationName := "Evolution Gaming"

organizationHomepage := Some(url("http://evolutiongaming.com"))

bintrayOrganization := Some("evolutiongaming")

scalaVersion := crossScalaVersions.value.head

crossScalaVersions := Seq("2.13.0", "2.12.10")

resolvers += Resolver.bintrayRepo("evolutiongaming", "maven")

libraryDependencies += compilerPlugin(`kind-projector` cross CrossVersion.binary)

libraryDependencies ++= Seq(
  Cats.core,
  Cats.effect,
  `cats-helper`,
  smetrics,
  scalatest % Test)

licenses := Seq(("MIT", url("https://opensource.org/licenses/MIT")))

releaseCrossBuild := true