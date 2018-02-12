package com.github.takezoe

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

import scala.concurrent.duration.Duration
import scala.concurrent.{Future, Promise}

object retry {

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
          Thread.sleep(if(config.backOff) config.retryDuration.toMillis * count else config.retryDuration.toMillis)
      }
    }
    ???
  }

  def retryAsync[T](f: => T)(implicit config: RetryConfig, retryManager: RetryManager): Future[T] = {
    retryManager.schedule(f)
  }

  case class RetryConfig(maxAttempts: Int, retryDuration: Duration, backOff: Boolean)

  class RetryManager {

    private val tasks = new ConcurrentLinkedQueue[RetryTask]()
    private val running = new AtomicBoolean(true)
    private val thread = new Thread {
      override def run(): Unit = {
        while(running.get()){
          val currentTime = System.currentTimeMillis
          tasks.iterator().forEachRemaining { task =>
            if(task.nextRun <= currentTime){
              tasks.remove(task)
              try {
                val result = task.f()
                task.promise.success(result)
              } catch {
                case e: Exception =>
                  if(task.count == task.config.maxAttempts){
                    task.promise.failure(e)
                  } else {
                    val count = task.count + 1
                    val nextRun = currentTime + (if(task.config.backOff) task.config.retryDuration.toMillis * count else task.config.retryDuration.toMillis)
                    tasks.add(new RetryTask(task.f, task.config, task.promise, nextRun, count))
                  }
              }
            }
          }
        }
        Thread.sleep(100)
      }
    }

    thread.start()

    def schedule[T](f: => T)(implicit config: RetryConfig): Future[T] = {
      val promise = Promise[T]()
      val task = new RetryTask(() => f, config, promise.asInstanceOf[Promise[Any]], System.currentTimeMillis + config.retryDuration.toMillis)
      tasks.add(task)
      promise.future
    }

    def shutdown(): Unit = {
      running.set(false)
    }

  }

  private class RetryTask(
    val f: () => Any,
    val config: RetryConfig,
    val promise: Promise[Any],
    val nextRun: Long,
    val count: Int = 0
  )

}
