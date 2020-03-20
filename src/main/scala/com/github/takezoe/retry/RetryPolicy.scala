package com.github.takezoe.retry

import scala.concurrent.duration._

case class RetryPolicy(
  maxAttempts: Int,
  retryDuration: FiniteDuration,
  backOff: BackOff = FixedBackOff,
  jitter: FiniteDuration = 0.second
)

object RetryPolicy {

  val NoRetry = RetryPolicy(0, Duration.Zero, FixedBackOff)

  def Immediately(attempts: Int): RetryPolicy = RetryPolicy(attempts, Duration.Zero, FixedBackOff)

}