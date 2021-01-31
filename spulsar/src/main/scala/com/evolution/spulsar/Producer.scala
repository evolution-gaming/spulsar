package com.evolution.spulsar

import cats.syntax.all._
import cats.effect.{Resource, Sync}
import org.apache.pulsar.client.api
import org.apache.pulsar.client.api.{MessageId, ProducerStats}

/**
  * See [[org.apache.pulsar.client.api.Producer]]
  */
trait Producer[F[_], A] {

  /**
    * @see [[org.apache.pulsar.client.api.Producer.getTopic]]
    */
  def topic: String

  /**
    * @see [[org.apache.pulsar.client.api.Producer.getProducerName]]
    */
  def producerName: String

  /**
    * @see [[org.apache.pulsar.client.api.Producer.sendAsync]]
    */
  def send(message: A): F[F[MessageId]]

  /**
    * @see [[org.apache.pulsar.client.api.Producer.flush]]
    */
  def flush: F[Unit]

  /**
    * @see [[org.apache.pulsar.client.api.Producer.getLastSequenceId]]
    */
  def lastSequenceId: F[Long]

  /**
    * @see [[org.apache.pulsar.client.api.Producer.getStats]]
    */
  def stats: F[ProducerStats]

  /**
    * @see [[org.apache.pulsar.client.api.Consumer.isConnected]]
    */
  def connected: F[Boolean]

  /**
    * @see [[org.apache.pulsar.client.api.Consumer.getLastDisconnectedTimestamp]]
    */
  def lastDisconnectedTimestamp: F[Long]
}

object Producer {
  def of[F[_]: Sync: FromCompletableFuture, A](
    producer: F[api.Producer[A]]
  ): Resource[F, Producer[F, A]] = {
    Resource
      .make {
        producer
      } { producer =>
        FromCompletableFuture[F].apply { producer.closeAsync() }.void
      }
      .map { producer => apply(producer) }
  }

  def apply[F[_]: Sync: FromCompletableFuture, A](
    producer: api.Producer[A]
  ): Producer[F, A] = {
    new Producer[F, A] {

      def topic = producer.getTopic

      def producerName = producer.getProducerName

      def send(message: A) = {
        Sync
          .apply[F]
          .delay { producer.sendAsync(message) }
          .map { future => FromCompletableFuture[F].apply { future } }
      }

      def flush = FromCompletableFuture[F].apply { producer.flushAsync() }.void

      def lastSequenceId = Sync[F].delay { producer.getLastSequenceId }

      def stats = Sync[F].delay { producer.getStats }

      def connected = Sync[F].delay { producer.isConnected }

      def lastDisconnectedTimestamp = {
        Sync[F].delay { producer.getLastDisconnectedTimestamp }
      }
    }
  }
}
