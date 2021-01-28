import Dependencies._

lazy val commonSettings = Seq(
  organization := "com.evolution",
  homepage := Some(new URL("http://github.com/evolution-gaming/spulsar")),
  startYear := Some(2019),
  organizationName := "Evolution Gaming",
  organizationHomepage := Some(url("http://evolutiongaming.com")),
  bintrayOrganization := Some("evolution"),
  scalaVersion := crossScalaVersions.value.head,
  crossScalaVersions := Seq("2.13.4", "2.12.12"),
  resolvers += Resolver.bintrayRepo("evolutiongaming", "maven"),
  licenses := Seq(("MIT", url("https://opensource.org/licenses/MIT"))),
  releaseCrossBuild := true,
  scalacOptsFailOnWarn := Some(false),
  scalacOptions in(Compile, doc) += "-no-link-warnings",
  libraryDependencies += compilerPlugin(`kind-projector` cross CrossVersion.binary))

lazy val root = (project
  in file(".")
  settings (name := "spulsar")
  settings (skip in publish := true)
  settings commonSettings
  aggregate(spulsar, tests))

lazy val spulsar = (project
  in file("spulsar")
  settings commonSettings
  settings(
    name := "spulsar",
    scalacOptions -= "-Ywarn-unused:params",
    libraryDependencies ++= Seq(
      Cats.core,
      Cats.effect,
      `cats-helper`,
      smetrics,
      Pulsar.client,
      Scalatest.scalatest % Test,
      Scalatest.funsuite % Test)))

lazy val tests = (project in file("tests")
  settings (name := "spulsar-tests")
  settings (skip in publish := true)
  dependsOn spulsar % "compile->compile;test->test"
  settings commonSettings
  settings Seq(
    skip in publish := true,
    Test / fork := true,
    Test / parallelExecution := false)
    dependsOn spulsar % "compile->compile;test->test"
    settings (libraryDependencies ++= Seq(
      Slf4j.api % Test,
      Slf4j.`log4j-over-slf4j` % Test,
      Logback.core % Test,
      Logback.classic % Test,
      Scalatest.scalatest % Test,
      Scalatest.funsuite % Test,
      Pureconfig.pureconfig % Test)))