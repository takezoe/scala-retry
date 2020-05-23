package com.github.takezoe.retry

import scala.concurrent.duration._
import java.util.concurrent.atomic.LongAdder
import java.util.concurrent.atomic.AtomicReference
import CircuitBreakerPolicy._

case class CircuitBreakerContext(
  state: State = Close,
  failureCount: Int = 0,
  successCount: Int = 0,
  lastFailure: Option[FailureInfo] = None
)

case class FailureInfo(
    timestampMillis: Long,
    exception: Throwable
)

class CircuitBreakerPolicy(
    val failureThreshold: Int,
    val successThreshold: Int,
    val retryDuration: FiniteDuration,
    val onClose: (CircuitBreakerContext) => Unit = _ => (),
    val onOpen: (CircuitBreakerContext) => Unit = _ => (),
    val onHalfOpen: (CircuitBreakerContext) => Unit = _ => ()
){
    private val context = new AtomicReference[CircuitBreakerContext](CircuitBreakerContext())

    def failed(e: Throwable): Unit = {
        // if failureThreashod <= 0 then the circuit breaker never closes.
        if (failureThreshold > 0) {
            val nextContext = context.get() match {
                case c @ CircuitBreakerContext(Close, failureCount, _, _) => 
                    if (failureCount + 1 >= failureThreshold) {
                        val nextContext = c.copy(state = Open, failureCount = failureCount + 1, successCount = 0, lastFailure = Some(FailureInfo(System.currentTimeMillis(), e)))
                        onOpen(nextContext)
                        nextContext
                    } else {
                        val nextContext = c.copy(failureCount = failureCount + 1)
                        nextContext
                    }
                case c @ CircuitBreakerContext(Open, failureCount, _, _) =>
                    val nextContext = c.copy(failureCount = failureCount + 1, successCount = 0)
                    nextContext
                case c @ CircuitBreakerContext(HalfOpen, failureCount, _, _) => 
                    val nextContext = c.copy(state = Open, failureCount = failureCount + 1, successCount = 0, lastFailure = Some(FailureInfo(System.currentTimeMillis(), e)))
                    onOpen(nextContext)
                    nextContext
            }
            context.set(nextContext)
        } 
    }

    def succeeded(): Unit = {
        // if failureThreashod <= 0 then the circuit breaker never closes.
        if (failureThreshold > 0) {
            val nextContext = context.get() match {
                case c @ CircuitBreakerContext(Close, _, successCount, _) =>
                  val nextContext = c.copy(failureCount = 0, successCount = successCount + 1)
                  nextContext
                case c @ CircuitBreakerContext(Open, _, successCount, _) =>
                    if (successCount + 1 >= successThreshold) {
                        val nextContext = c.copy(state = Close, failureCount = 0, successCount = successCount + 1, lastFailure = None)
                        onClose(nextContext)
                        nextContext
                    } else {
                        val nextContext = c.copy(state = HalfOpen, failureCount = 0, successCount = successCount + 1, lastFailure = None)
                        onHalfOpen(nextContext)
                        nextContext
                    }
                case c @ CircuitBreakerContext(HalfOpen, _, successCount, _) =>
                    if (successCount + 1 >= successThreshold) {
                        val nextContext = c.copy(state = Close, failureCount = 0, successCount = successCount + 1, lastFailure = None)
                        onClose(nextContext)
                        nextContext
                    } else {
                        val nextContext = c.copy(failureCount = 0, successCount = successCount + 1)
                        nextContext
                    }
            }
            context.set(nextContext)
        }
    }

    def getContext(): CircuitBreakerContext = context.get()
}

object CircuitBreakerPolicy {
    sealed trait State
    case object Open extends State
    case object Close extends State
    case object HalfOpen extends State

    val NeverOpen = new CircuitBreakerPolicy(0, 0, 0.second)

    def apply(
        failureThreshold: Int, 
        successThreshold: Int, 
        retryDuration: FiniteDuration,
        onClose: (CircuitBreakerContext) => Unit = _ => (),
        onOpen: (CircuitBreakerContext) => Unit = _ => (),
        onHalfOpen: (CircuitBreakerContext) => Unit = _ => ()        
    ): CircuitBreakerPolicy = {
        new CircuitBreakerPolicy(
            failureThreshold = failureThreshold,
            successThreshold = successThreshold,
            retryDuration = retryDuration,
            onClose = onClose,
            onOpen = onOpen,
            onHalfOpen = onHalfOpen
        )
    }

}