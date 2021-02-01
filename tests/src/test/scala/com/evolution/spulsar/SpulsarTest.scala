package com.evolution.spulsar

import cats.effect.IO
import cats.syntax.all._
import com.evolution.spulsar.IOSuite._
import com.evolutiongaming.catshelper.CatsHelper._
import org.apache.pulsar.client.api.{Message, Schema}
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers
import pureconfig.error.ConfigReaderException
import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.generic.semiauto.deriveReader

import scala.concurrent.duration._

import java.util.UUID

class SpulsarTest extends AsyncFunSuite with Matchers {
  import SpulsarTest._

  test("produce and consume") {
    val config = ConfigSource
      .default
      .at("evolution.spulsar.client")
      .load[Config] match {
      case Right(a) => a.pure[IO]
      case Left(a) =>
        new ConfigReaderException[Config](a).raiseError[IO, Config]
    }

    val result = for {
      topic  <- IO { UUID.randomUUID().toString }.toResource
      config <- config.toResource
      client <- Client.of(s"pulsar://${config.host}:${config.port}")
      name    = "SpulsarTest"
      producer <- client.producer(Schema.STRING) { config =>
        config
          .topic(topic)
          .producerName(name)
      }
      consumer <- client.consumer(Schema.STRING) { config =>
        config
          .topic(topic)
          .consumerName(name)
          .subscriptionName(name)
      }
    } yield for {
      connected <- producer.connected
      _         <- IO { connected shouldEqual true }
      connected <- consumer.connected
      _         <- IO { connected shouldEqual true }
      _         <- IO { producer.name shouldEqual name }
      _         <- IO { consumer.name shouldEqual name }
      _         <- IO { producer.topic shouldEqual topic }
      _         <- IO { consumer.topic shouldEqual topic }
      _         <- IO { consumer.subscription shouldEqual name }
      value     <- IO { UUID.randomUUID().toString }
      fiber <- {
        consumer
          .receive
          .map { message =>
            if (message.getValue == value) message.some
            else none[Message[String]]
          }
          .untilDefinedM
          .timeout(5.seconds)
          .start
      }
      messageId            <- producer.send(value).flatten
      _                    <- producer.flush
      lastSequenceId       <- producer.lastSequenceId
      message              <- fiber.join
      _                    <- consumer.acknowledge(message)
      _                    <- consumer.pause
      _                    <- consumer.resume
      _                    <- IO { message.getMessageId shouldEqual messageId }
      _                    <- IO { message.getSequenceId shouldEqual lastSequenceId }
      hasReachedEndOfTopic <- consumer.hasReachedEndOfTopic
//      _                    <- IO { hasReachedEndOfTopic shouldEqual true }
      lastMessageId        <- consumer.lastMessageId
//      _                    <- IO { lastMessageId shouldEqual messageId }
      _                    <- consumer.unsubscribe
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
