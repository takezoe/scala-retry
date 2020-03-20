package com.github.takezoe

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Random, Success, Try}

package object retry {

import java.util.concurrent.ThreadLocalRandom

  def retryBlocking[T](f: => T)(implicit policy: RetryPolicy): T = {
    var count = 0

    while(true){
      try {
        return f
      } catch {
        case NonFatal(e) =>
          if(count == policy.maxAttempts){
            policy.onFailure(e)
            throw e
          }
          policy.onRetry(e)
          count = count + 1
          Thread.sleep(
            policy.backOff.nextDuration(count, policy.retryDuration.toMillis) + jitter(policy.jitter.toMillis)
          )
      }
    }
    ??? // never come to here
  }

  def retryBlockingAsEither[T](f: => T)(implicit policy: RetryPolicy): Either[Throwable, T] = {
    try {
      val result = retryBlocking(f)
      Right(result)
    } catch {
      case NonFatal(e) => Left(e)
    }
  }

  def retryBlockingAsTry[T](f: => T)(implicit policy: RetryPolicy): Try[T] = {
    try {
      val result = retryBlocking(f)
      Success(result)
    } catch {
      case NonFatal(e) => Failure(e)
    }
  }

  def retryFuture[T](f: => Future[T])(implicit policy: RetryPolicy, retryManager: RetryManager, ec: ExecutionContext): Future[T] = {
    val future = f
    if(policy.maxAttempts > 0){
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
      (ThreadLocalRandom.current().nextDouble() * maxMills).toLong
    }
  }

  def circuitBreaker[T](f: => T)(implicit policy: CircuitBreakerPolicy): T = {
    import CircuitBreakerPolicy._
    def run(): T = {
        try {
          val result = f
          policy.succeeded()
          result
        } catch {
          case NonFatal(e) =>
            policy.failed(e)
            throw e
        }
    }

    policy.getState() match {
      case (Open, Some((lastFailureTimeMillis, lastException))) => {
        if (System.currentTimeMillis - lastFailureTimeMillis >= policy.retryDuration.toMillis) {
          run()
        } else {
          throw lastException
        }
      }
      case _ => run()
    }
  }

  def circuitBreakerAsEither[T](f: => T)(implicit policy: CircuitBreakerPolicy): Either[Throwable, T] = {
    try {
      val result = circuitBreaker(f)
      Right(result)
    } catch {
      case NonFatal(e) => Left(e) 
    }
  }

  def circuitBreakerAsTry[T](f: => T)(implicit policy: CircuitBreakerPolicy): Try[T] = {
    try {
      val result = circuitBreaker(f)
      Success(result)
    } catch {
      case NonFatal(e) => Failure(e) 
    }
  }
}
