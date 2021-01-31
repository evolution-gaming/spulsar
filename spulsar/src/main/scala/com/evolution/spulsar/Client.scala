package com.evolution.spulsar

import cats.Monad
import cats.effect.{Resource, Sync}
import cats.syntax.all._
import org.apache.pulsar.client.api._

trait Client[F[_]] {

  def producer[A](schema: Schema[A])(f: ProducerBuilder[A] => ProducerBuilder[A]): Resource[F, Producer[A]]

  def consumer[A](schema: Schema[A])(f: ConsumerBuilder[A] => ConsumerBuilder[A]): Resource[F, Consumer[A]]
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

          def producer[A](schema: Schema[A])(f: ProducerBuilder[A] => ProducerBuilder[A]) = {
            val producer = f(pulsarClient.newProducer(schema))
            Resource.make {
              FromCompletableFuture[F].apply { producer.createAsync() }
            } { producer =>
              FromCompletableFuture[F].apply { producer.closeAsync() }.void
            }
          }

          def consumer[A](schema: Schema[A])(f: ConsumerBuilder[A] => ConsumerBuilder[A]) = {
            val consumer = f(pulsarClient.newConsumer(schema))
            Resource.make {
              FromCompletableFuture[F].apply { consumer.subscribeAsync() }
            } { consumer =>
              FromCompletableFuture[F].apply { consumer.closeAsync() }.void
            }
          }
        }
      }
  }
}
