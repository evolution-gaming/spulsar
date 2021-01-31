package com.evolution.spulsar

import cats.effect.{Resource, Sync}
import cats.syntax.all._
import org.apache.pulsar.client.api.{
  ConsumerBuilder,
  ProducerBuilder,
  PulsarClient,
  Schema
}

trait Client[F[_]] {

  def producer[A](schema: Schema[A])(
    f: ProducerBuilder[A] => ProducerBuilder[A]
  ): Resource[F, Producer[F, A]]

  def consumer[A](schema: Schema[A])(
    f: ConsumerBuilder[A] => ConsumerBuilder[A]
  ): Resource[F, Consumer[F, A]]
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

  def of[F[_]: Sync: FromCompletableFuture](
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

          def producer[A](
            schema: Schema[A]
          )(f: ProducerBuilder[A] => ProducerBuilder[A]) = {
            val producer = f(pulsarClient.newProducer(schema))
            Producer.of(FromCompletableFuture[F].apply {
              producer.createAsync()
            })
          }

          def consumer[A](
            schema: Schema[A]
          )(f: ConsumerBuilder[A] => ConsumerBuilder[A]) = {
            val consumer = f(pulsarClient.newConsumer(schema))
            Consumer.of(FromCompletableFuture[F].apply {
              consumer.subscribeAsync()
            })
          }
        }
      }
  }
}
