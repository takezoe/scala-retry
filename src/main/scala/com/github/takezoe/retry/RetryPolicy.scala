package com.github.takezoe.retry

import scala.concurrent.duration._

case class RetryPolicy(
  maxAttempts: Int,
  retryDuration: FiniteDuration,
  backOff: BackOff = FixedBackOff,
  jitter: FiniteDuration = 0.second,
  onRetry: RetryContext => Unit = _ => (),
  onFailure: RetryContext => Unit = _ => ()
)

case class RetryContext(
  retryCount: Int,
  exception: Throwable
)

object RetryPolicy {

  val NoRetry = RetryPolicy(0, Duration.Zero, FixedBackOff)

  def Immediately(attempts: Int): RetryPolicy = RetryPolicy(attempts, Duration.Zero, FixedBackOff)

}