package com.evolution.spulsar

import cats.effect.{IO, Timer}
import cats.implicits.{catsSyntaxApplicativeErrorId, catsSyntaxApplicativeId}
import com.evolution.spulsar.IOSuite._
import com.evolutiongaming.catshelper.CatsHelper._
import org.scalatest.funsuite.AsyncFunSuite
import pureconfig.error.ConfigReaderException
import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.generic.semiauto.deriveReader

import scala.concurrent.duration._

class SpulsarTest extends AsyncFunSuite {
  import SpulsarTest._

  test("produce and consume") {
    val config = ConfigSource.default
      .at("evolution.spulsar.client")
      .load[Config] match {
        case Right(a) => a.pure[IO]
        case Left(a)  => new ConfigReaderException[Config](a).raiseError[IO, Config]
    }

    val result = for {
      config   <- config.toResource
      _        <- IO { println(config) }.toResource
      client   <- Client.of(s"pulsar://${config.host}:${config.port}")
      producer <- client.producer("topic")
    } yield for {
      _ <- Timer[IO].sleep(1.second)
      a <- IO { producer.getProducerName }
      _ <- IO { println(a) }
    } yield {}
    result.use { a => a }.run()
  }
}

object SpulsarTest {
  final case class Config(host: String = "localhost", port: Int = 6650)

  object Config {
    implicit val configReaderConfig: ConfigReader[Config] = deriveReader
  }
}
