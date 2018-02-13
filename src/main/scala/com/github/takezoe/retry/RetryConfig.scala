package com.github.takezoe.retry

import scala.concurrent.duration.FiniteDuration

case class RetryConfig(
  maxAttempts: Int,
  retryDuration: FiniteDuration,
  backOff: BackOff
)