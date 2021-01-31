package com.evolution.spulsar

import cats.effect.{Concurrent, Sync}
import cats.syntax.all._

import java.util.concurrent.{CompletableFuture, CompletionException}
import scala.concurrent.{CancellationException, ExecutionException}
import scala.util.control.NonFatal

trait FromCompletableFuture[F[_]] {

  def apply[A](a: => CompletableFuture[A]): F[A]
}

object FromCompletableFuture {

  def apply[F[_]](
    implicit F: FromCompletableFuture[F]
  ): FromCompletableFuture[F] = F

  implicit def concurrentCompletableFuture[F[_]: Concurrent]
    : FromCompletableFuture[F] = {
    new FromCompletableFuture[F] {
      def apply[A](a: => CompletableFuture[A]) = {
        Sync[F].delay { a }.flatMap { a =>
          if (a.isDone) {
            try a.get().pure[F]
            catch {
              case a: ExecutionException if a.getCause ne null =>
                a.getCause.raiseError[F, A]
              case NonFatal(a) => a.raiseError[F, A]
            }
          } else {
            Concurrent[F].cancelable[A] { f =>
              a.handle[Unit] { (a: A, e: Throwable) =>
                e match {
                  case null                     => f(a.asRight[Throwable])
                  case _: CancellationException => ()
                  case a: CompletionException if a.getCause ne null =>
                    f(a.getCause.asLeft[A])
                  case a: Throwable => f(a.asLeft[A])
                }
              }
              Sync[F].delay {
                a.cancel(true)
                ()
              }
            }
          }
        }
      }
    }
  }
}
