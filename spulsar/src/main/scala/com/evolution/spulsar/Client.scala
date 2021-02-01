package com.evolution.spulsar

import cats.effect.{Resource, Sync}
import cats.syntax.all._
import org.apache.pulsar.client.api.{ClientBuilder, ConsumerBuilder, ProducerBuilder, PulsarClient, Schema}

trait Client[F[_]] {

  def producer[A](schema: Schema[A])(
    f: ProducerBuilder[A] => ProducerBuilder[A]
  ): Resource[F, Producer[F, A]]

  def consumer[A](schema: Schema[A])(
    f: ConsumerBuilder[A] => ConsumerBuilder[A]
  ): Resource[F, Consumer[F, A]]
}

object Client {

  def fromClientBuilder[F[_]: Sync: FromCompletableFuture](
    f: ClientBuilder => ClientBuilder
  ): Resource[F, Client[F]] = {
    val pulsarClient = Sync[F].delay { f(PulsarClient.builder()).build() }
    fromPulsarClient(pulsarClient)
  }

  def fromPulsarClient[F[_]: Sync: FromCompletableFuture](
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
            val builder  = f(pulsarClient.newProducer(schema))
            val producer = FromCompletableFuture[F].apply { builder.createAsync() }
            Producer.of(producer)
          }

          def consumer[A](schema: Schema[A])(f: ConsumerBuilder[A] => ConsumerBuilder[A]) = {
            val builder  = f(pulsarClient.newConsumer(schema))
            val consumer = FromCompletableFuture[F].apply { builder.subscribeAsync() }
            Consumer.of(consumer)
          }
        }
      }
  }
}
