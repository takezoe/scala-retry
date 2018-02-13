package com.github.takezoe

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

package object retry {

  def retryBlocking[T](f: => T)(implicit config: RetryConfig): T = {
    var count = 0

    while(true){
      try {
        return f
      } catch {
        case e: Exception =>
          if(count == config.maxAttempts){
            throw e
          }
          count = count + 1
          Thread.sleep(config.backOff.nextDuration(count, config.retryDuration.toMillis))
      }
    }
    ???
  }

  def retryBlockingAsEither[T](f: => T)(implicit config: RetryConfig): Either[Exception, T] = {
    try {
      val result = retryBlocking(f)
      Right(result)
    } catch {
      case e: Exception => Left(e)
    }
  }

  def retryBlockingAsTry[T](f: => T)(implicit config: RetryConfig): Try[T] = {
    try {
      val result = retryBlocking(f)
      Success(result)
    } catch {
      case e: Exception => Failure(e)
    }
  }

  def retryAsync[T](f: => T)(implicit config: RetryConfig, retryManager: RetryManager): Future[T] = {
    retryManager.schedule(f)
  }

  def retryFuture[T](f: => Future[T])(implicit config: RetryConfig, retryManager: RetryManager, ec: ExecutionContext): Future[T] = {
    retryManager.scheduleFuture(f)
  }

}
