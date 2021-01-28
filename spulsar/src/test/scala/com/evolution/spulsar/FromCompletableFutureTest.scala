package com.evolution.spulsar

import cats.effect.IO
import cats.syntax.all._
import com.evolution.spulsar.IOSuite._
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

import java.util.concurrent.CompletableFuture
import scala.concurrent.TimeoutException
import scala.concurrent.duration._
import scala.util.control.NoStackTrace

class FromCompletableFutureTest extends AsyncFunSuite with Matchers {

  test("not completed future") {
    val result = for {
      future <- IO { new CompletableFuture[String] }
      fiber  <- FromCompletableFuture[IO].apply { future }.start
      a      <- fiber.join.timeout(1.millis).attempt
      _      <- IO { a should matchPattern { case Left(_: TimeoutException ) => } }
      _      <- IO { future.complete("value") }
      a      <- fiber.join
      _      <- IO { a shouldEqual "value" }
    } yield {}
    result.run()
  }

  test("successful future") {
    val future = CompletableFuture.completedFuture("value")
    val result = for {
      a      <- FromCompletableFuture[IO].apply { future }
      _      <- IO { a shouldEqual "value" }
    } yield {}
    result.run()
  }

  test("failed future") {
    val result = for {
      error  <- IO { new RuntimeException() with NoStackTrace: Throwable }
      future  = CompletableFuture.failedFuture[Unit](error)
      a      <- FromCompletableFuture[IO].apply { future }.attempt
      _      <- IO { a shouldEqual error.asLeft }
    } yield {}
    result.run()
  }
}
