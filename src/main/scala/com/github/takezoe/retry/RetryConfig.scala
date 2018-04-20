package com.github.takezoe.retry

import scala.concurrent.duration.{Duration, FiniteDuration}

case class RetryConfig(
  maxAttempts: Int,
  retryDuration: FiniteDuration,
  backOff: BackOff
)

object RetryConfig {

  val NoRetry = RetryConfig(0, Duration.Zero, FixedBackOff)

  def Immediately(attempts: Int): RetryConfig = RetryConfig(attempts, Duration.Zero, FixedBackOff)

}