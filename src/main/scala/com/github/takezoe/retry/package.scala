package com.github.takezoe

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Random, Success, Try}

package object retry {

  private val r = new Random()

  def retryBlocking[T](f: => T)(implicit config: RetryConfig): T = {
    var count = 0

    while(true){
      try {
        return f
      } catch {
        case NonFatal(e) =>
          if(count == config.maxAttempts){
            throw e
          }
          count = count + 1
          Thread.sleep(
            config.backOff.nextDuration(count, config.retryDuration.toMillis) + jitter(config.jitter.toMillis)
          )
      }
    }
    ??? // never come here
  }

  def retryBlockingAsEither[T](f: => T)(implicit config: RetryConfig): Either[Throwable, T] = {
    try {
      val result = retryBlocking(f)
      Right(result)
    } catch {
      case NonFatal(e) => Left(e)
    }
  }

  def retryBlockingAsTry[T](f: => T)(implicit config: RetryConfig): Try[T] = {
    try {
      val result = retryBlocking(f)
      Success(result)
    } catch {
      case NonFatal(e) => Failure(e)
    }
  }

  def retryFuture[T](f: => Future[T])(implicit config: RetryConfig, retryManager: RetryManager, ec: ExecutionContext): Future[T] = {
    val future = f
    if(config.maxAttempts > 0){
      future.recoverWith { case _ =>
        retryManager.scheduleFuture(f)
      }
    } else {
      future
    }
  }

  private[retry] def jitter(maxMills: Long): Long = {
    if(maxMills == 0){
      0
    } else {
      (r.nextDouble() * maxMills).toLong
    }
  }

}
