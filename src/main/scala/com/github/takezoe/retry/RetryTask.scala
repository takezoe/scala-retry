package com.github.takezoe.retry

import scala.concurrent.{ExecutionContext, Future, Promise}

private[retry] sealed trait RetryTask {
  val nextRun: Long
}

private[retry] class BlockingRetryTask(
  val f: () => Any,
  val config: RetryConfig,
  val promise: Promise[Any],
  val nextRun: Long,
  val count: Int = 0
) extends RetryTask

private[retry] class FutureRetryTask(
  val f: () => Future[Any],
  val config: RetryConfig,
  val ec: ExecutionContext,
  val promise: Promise[Any],
  val nextRun: Long,
  val count: Int = 0
) extends RetryTask