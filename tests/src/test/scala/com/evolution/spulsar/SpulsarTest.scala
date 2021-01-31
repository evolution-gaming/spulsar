package com.evolution.spulsar

import cats.effect.{IO, Timer}
import cats.syntax.all._
import com.evolution.spulsar.IOSuite._
import com.evolutiongaming.catshelper.CatsHelper._
import org.apache.pulsar.client.api.{Message, Schema}
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers
import pureconfig.error.ConfigReaderException
import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.generic.semiauto.deriveReader

import java.util.UUID
import scala.concurrent.duration._

class SpulsarTest extends AsyncFunSuite with Matchers {
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
      topic    = "topic"
      producer <- client.producer(Schema.STRING) { config =>
        config
          .topic(topic)
          .producerName("SpulsarTest")
      }
      consumer <- client.consumer(Schema.STRING) { config =>
        config
          .topic(topic)
          .consumerName("SpulsarTest")
          .subscriptionName("SpulsarTest")
      }
    } yield for {
      value <- IO { UUID.randomUUID().toString }
      fiber <- 10
        .tailRecM { n =>
        if (n <= 0) {
          none[Message[String]].asRight[Int].pure[IO]
        } else {
          for {
            message <- FromCompletableFuture[IO].apply { consumer.receiveAsync() }
          } yield {
            if (message.getValue == value) {
              message.some.asRight[Int]
            } else {
              (n - 1).asLeft[Option[Message[String]]]
            }
          }
        }
      }
        .start
      messageId <- FromCompletableFuture[IO].apply { producer.sendAsync(value) }
      _ <- IO { println(s"messageId: $messageId") }
      message <- fiber.join
      _ <- IO { message.map { _.getMessageId } shouldEqual messageId.some }
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
