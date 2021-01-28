package com.evolution.spulsar

import cats.Monad
import cats.effect.{Resource, Sync}
import cats.syntax.all._
import org.apache.pulsar.client.api.{Producer, PulsarClient}

trait Client[F[_]] {
  def producer(topic: String): Resource[F, Producer[Array[Byte]] /*TODO*/ ]
}

object Client {

  def of[F[_]: Sync: FromCompletableFuture](
    serviceUrl: String
  ): Resource[F, Client[F]] = {
    val pulsarClient = Sync[F].delay {
      PulsarClient
        .builder()
        .serviceUrl(serviceUrl)
        .build()
    }
    of(pulsarClient)
  }

  def of[F[_]: Monad: FromCompletableFuture](
    pulsarClient: F[PulsarClient]
  ): Resource[F, Client[F]] = {
    Resource
      .make {
        pulsarClient
      } { pulsarClient =>
        FromCompletableFuture[F].apply { pulsarClient.closeAsync() }.void
      }
      .map { pulsarClient =>
        new Client[F] {
          def producer(topic: String) = {
            val producer = pulsarClient
              .newProducer()
              .topic(topic)
            Resource.make {
              FromCompletableFuture[F].apply { producer.createAsync() }
            } { producer =>
              FromCompletableFuture[F].apply { producer.closeAsync() }.void
            }
          }
        }
      }
  }
}
