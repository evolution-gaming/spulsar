import sbt._

object Dependencies {

  val scalatest        = "org.scalatest"       %% "scalatest"      % "3.2.3"
  val `cats-helper`    = "com.evolutiongaming" %% "cats-helper"    % "2.2.1"
  val smetrics         = "com.evolutiongaming" %% "smetrics"       % "0.2.0"
  val `kind-projector` = "org.typelevel"        % "kind-projector" % "0.10.3"

  object Cats {
    private val version = "2.3.1"
    val core   = "org.typelevel" %% "cats-core"   % version
    val effect = "org.typelevel" %% "cats-effect" % version
  }

  object Logback {
    private val version = "1.2.3"
    val core    = "ch.qos.logback" % "logback-core"    % version
    val classic = "ch.qos.logback" % "logback-classic" % version
  }

  object Slf4j {
    private val version = "1.7.30"
    val api                = "org.slf4j" % "slf4j-api"        % version
    val `log4j-over-slf4j` = "org.slf4j" % "log4j-over-slf4j" % version
  }

  object Pulsar {
    private val version = "2.7.0"
    val client = "org.apache.pulsar" % "pulsar-client" % version
  }
}