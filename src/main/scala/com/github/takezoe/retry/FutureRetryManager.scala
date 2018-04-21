package com.github.takezoe.retry

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

class RetryManager {

  private val tasks = new ConcurrentLinkedQueue[FutureRetryTask]()

  private val running = new AtomicBoolean(true)

  private val thread = new Thread {
    override def run(): Unit = {
      while(running.get()){
        val currentTime = System.currentTimeMillis
        tasks.iterator().forEachRemaining { task =>
          if(task.nextRun <= currentTime){
            tasks.remove(task)
            val future = task.f()
            future.onComplete {
              case Success(v) => task.promise.success(v)
              case Failure(e) => {
                if(task.count == task.config.maxAttempts){
                  task.promise.failure(e)
                } else {
                  val count = task.count + 1
                  val nextRun = currentTime + task.config.backOff.nextDuration(count, task.config.retryDuration.toMillis)
                  tasks.add(new FutureRetryTask(task.f, task.config, task.ec, task.promise, nextRun, count))
                }
              }
            }(task.ec)
          }
        }
      }
      Thread.sleep(100)
    }
  }

  thread.start()

  def scheduleFuture[T](f: => Future[T])(implicit config: RetryConfig, ec: ExecutionContext): Future[T] = {
    val promise = Promise[T]()
    val task = new FutureRetryTask(() => f, config, ec, promise.asInstanceOf[Promise[Any]], System.currentTimeMillis + config.retryDuration.toMillis)
    tasks.add(task)
    promise.future
  }

  def shutdown(): Unit = {
    running.set(false)
  }

}

private[retry] class FutureRetryTask(
  val f: () => Future[Any],
  val config: RetryConfig,
  val ec: ExecutionContext,
  val promise: Promise[Any],
  val nextRun: Long,
  val count: Int = 1
)