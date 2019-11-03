package com.github.takezoe.retry

import scala.concurrent.duration._

case class RetryConfig(
  maxAttempts: Int,
  retryDuration: FiniteDuration,
  backOff: BackOff = FixedBackOff,
  jitter: FiniteDuration = 0.second
)

object RetryConfig {

  val NoRetry = RetryConfig(0, Duration.Zero, FixedBackOff)

  def Immediately(attempts: Int): RetryConfig = RetryConfig(attempts, Duration.Zero, FixedBackOff)

}