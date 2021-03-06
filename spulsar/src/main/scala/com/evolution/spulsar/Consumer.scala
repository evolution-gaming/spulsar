package com.evolution.spulsar

import cats.effect.{Resource, Sync}
import cats.syntax.all._
import org.apache.pulsar.client.api
import org.apache.pulsar.client.api.transaction.Transaction
import org.apache.pulsar.client.api.{ConsumerStats, Message, MessageId, Messages}

import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters._

/**
  * @see [[org.apache.pulsar.client.api.Consumer]]
  */
trait Consumer[F[_], A] {

  /**
    * @see [[org.apache.pulsar.client.api.Consumer.getTopic]]
    */
  def topic: String;

  /**
    * @see [[org.apache.pulsar.client.api.Consumer.getSubscription]]
    */
  def subscription: String

  /**
    * @see [[org.apache.pulsar.client.api.Consumer.unsubscribeAsync]]
    */
  def unsubscribe: F[Unit]

  /**
    * @see [[org.apache.pulsar.client.api.Consumer.receiveAsync]]
    */
  def receive: F[Message[A]]

  /**
    * @see [[org.apache.pulsar.client.api.Consumer.batchReceiveAsync]]
    */
  def batchReceive: F[Messages[A]]

  /**
    * @see [[org.apache.pulsar.client.api.Consumer.negativeAcknowledge]]
    */
  def negativeAcknowledge(message: Message[_]): F[Unit]

  /**
    * @see [[org.apache.pulsar.client.api.Consumer.negativeAcknowledge]]
    */
  def negativeAcknowledge(messageId: MessageId): F[Unit]

  /**
    * @see [[org.apache.pulsar.client.api.Consumer.negativeAcknowledge]]
    */
  def negativeAcknowledge(messages: Messages[_]): F[Unit]

  /**
    * @see [[org.apache.pulsar.client.api.Consumer.acknowledgeAsync]]
    */
  def acknowledge(message: Message[_]): F[Unit]

  /**
    * @see [[org.apache.pulsar.client.api.Consumer.acknowledgeAsync]]
    */
  def acknowledge(messageId: MessageId): F[Unit]

  /**
    * @see [[org.apache.pulsar.client.api.Consumer.acknowledgeAsync]]
    */
  def acknowledge(messageId: MessageId, transaction: Transaction): F[Unit]

  /**
    * @see [[org.apache.pulsar.client.api.Consumer.acknowledgeAsync]]
    */
  def acknowledge(messages: Messages[_]): F[Unit]

  /**
    * @see [[org.apache.pulsar.client.api.Consumer.acknowledgeAsync]]
    */
  def acknowledge(messageIds: List[MessageId]): F[Unit]

  /**
    * @see [[org.apache.pulsar.client.api.Consumer.reconsumeLaterAsync]]
    */
  def reconsumeLater(message: Message[_], delay: FiniteDuration): F[Unit]

  /**
    * @see [[org.apache.pulsar.client.api.Consumer.reconsumeLaterAsync]]
    */
  def reconsumeLater(messages: Messages[_], delay: FiniteDuration): F[Unit]

  /**
    * @see [[org.apache.pulsar.client.api.Consumer.acknowledgeCumulativeAsync]]
    */
  def acknowledgeCumulative(message: Message[_]): F[Unit]

  /**
    * @see [[org.apache.pulsar.client.api.Consumer.acknowledgeCumulativeAsync]]
    */
  def acknowledgeCumulative(messageId: MessageId): F[Unit]

  /**
    * @see [[org.apache.pulsar.client.api.Consumer.acknowledgeCumulativeAsync]]
    */
  def acknowledgeCumulative(
    messageId: MessageId,
    transaction: Transaction
  ): F[Unit]

  /**
    * @see [[org.apache.pulsar.client.api.Consumer.reconsumeLaterCumulativeAsync]]
    */
  def reconsumeLaterCumulative(
    message: Message[_],
    delay: FiniteDuration
  ): F[Unit]

  /**
    * @see [[org.apache.pulsar.client.api.Consumer.getStats]]
    */
  def stats: F[ConsumerStats]

  /**
    * @see [[org.apache.pulsar.client.api.Consumer.hasReachedEndOfTopic]]
    */
  def hasReachedEndOfTopic: F[Boolean]

  /**
    * @see [[org.apache.pulsar.client.api.Consumer.redeliverUnacknowledgedMessages]]
    */
  def redeliverUnacknowledgedMessages: F[Unit]

  /**
    * @see [[org.apache.pulsar.client.api.Consumer.seekAsync]]
    */
  def seek(messageId: MessageId): F[Unit]

  /**
    * @see [[org.apache.pulsar.client.api.Consumer.seekAsync]]
    */
  def seek(timestamp: Long): F[Unit]

  /**
    * @see [[org.apache.pulsar.client.api.Consumer.getLastMessageIdAsync]]
    */
  def lastMessageId: F[MessageId]

  /**
    * @see [[org.apache.pulsar.client.api.Consumer.isConnected]]
    */
  def connected: F[Boolean]

  /**
    * @see [[org.apache.pulsar.client.api.Consumer.getConsumerName]]
    */
  def name: String

  /**
    * @see [[org.apache.pulsar.client.api.Consumer.pause]]
    */
  def pause: F[Unit]

  /**
    * @see [[org.apache.pulsar.client.api.Consumer.resume]]
    */
  def resume: F[Unit]

  /**
    * @see [[org.apache.pulsar.client.api.Consumer.getLastDisconnectedTimestamp]]
    */
  def lastDisconnectedTimestamp: F[Long]
}

object Consumer {

  def of[F[_]: Sync: FromCompletableFuture, A](
    consumer: F[api.Consumer[A]]
  ): Resource[F, Consumer[F, A]] = {
    Resource
      .make {
        consumer
      } { consumer =>
        FromCompletableFuture[F].apply { consumer.closeAsync() }.void
      }
      .map { consumer => apply(consumer) }
  }

  def apply[F[_]: Sync: FromCompletableFuture, A](
    consumer: api.Consumer[A]
  ): Consumer[F, A] = {

    val fromCompletableFuture = FromCompletableFuture[F]

    new Consumer[F, A] {

      def topic = consumer.getTopic

      def subscription = consumer.getSubscription

      def unsubscribe = {
        fromCompletableFuture { consumer.unsubscribeAsync() }.void
      }

      def receive = fromCompletableFuture { consumer.receiveAsync() }

      def batchReceive = {
        fromCompletableFuture { consumer.batchReceiveAsync() }
      }

      def negativeAcknowledge(message: Message[_]) = {
        Sync[F].delay { consumer.negativeAcknowledge(message) }
      }

      def negativeAcknowledge(messageId: MessageId) = {
        Sync[F].delay { consumer.negativeAcknowledge(messageId) }
      }

      def negativeAcknowledge(messages: Messages[_]) = {
        Sync[F].delay { consumer.negativeAcknowledge(messages) }
      }

      def acknowledge(message: Message[_]) = {
        fromCompletableFuture { consumer.acknowledgeAsync(message) }.void
      }

      def acknowledge(messageId: MessageId) = {
        fromCompletableFuture { consumer.acknowledgeAsync(messageId) }.void
      }

      def acknowledge(messageId: MessageId, transaction: Transaction) = {
        fromCompletableFuture {
          consumer.acknowledgeAsync(messageId, transaction)
        }.void
      }

      def acknowledge(messages: Messages[_]) = {
        fromCompletableFuture { consumer.acknowledgeAsync(messages) }.void
      }

      def acknowledge(messageIds: List[MessageId]) = {
        fromCompletableFuture {
          consumer.acknowledgeAsync(messageIds.asJava)
        }.void
      }

      def reconsumeLater(message: Message[_], delay: FiniteDuration) = {
        fromCompletableFuture.apply {
          consumer.reconsumeLaterAsync(message, delay.length, delay.unit)
        }.void
      }

      def reconsumeLater(messages: Messages[_], delay: FiniteDuration) = {
        fromCompletableFuture.apply {
          consumer.reconsumeLaterAsync(messages, delay.length, delay.unit)
        }.void
      }

      def acknowledgeCumulative(message: Message[_]) = {
        fromCompletableFuture {
          consumer.acknowledgeCumulativeAsync(message)
        }.void
      }

      def acknowledgeCumulative(messageId: MessageId) = {
        fromCompletableFuture {
          consumer.acknowledgeCumulativeAsync(messageId)
        }.void
      }

      def acknowledgeCumulative(
        messageId: MessageId,
        transaction: Transaction
      ) = {
        fromCompletableFuture {
          consumer.acknowledgeCumulativeAsync(messageId, transaction)
        }.void
      }

      def reconsumeLaterCumulative(
        message: Message[_],
        delay: FiniteDuration
      ) = {
        fromCompletableFuture.apply {
          consumer.reconsumeLaterCumulativeAsync(
            message,
            delay.length,
            delay.unit
          )
        }.void
      }

      def stats = Sync[F].delay { consumer.getStats }

      def hasReachedEndOfTopic = Sync[F].delay { consumer.hasReachedEndOfTopic }

      def redeliverUnacknowledgedMessages = {
        Sync[F].delay { consumer.redeliverUnacknowledgedMessages() }
      }

      def seek(messageId: MessageId) = {
        fromCompletableFuture { consumer.seekAsync(messageId) }.void
      }

      def seek(timestamp: Long) = {
        fromCompletableFuture { consumer.seekAsync(timestamp) }.void
      }

      def lastMessageId = {
        fromCompletableFuture { consumer.getLastMessageIdAsync }
      }

      def connected = Sync[F].delay { consumer.isConnected }

      def name = consumer.getConsumerName

      def pause = Sync[F].delay { consumer.pause() }

      def resume = Sync[F].delay { consumer.resume() }

      def lastDisconnectedTimestamp = {
        Sync[F].delay { consumer.getLastDisconnectedTimestamp }
      }
    }
  }
}
