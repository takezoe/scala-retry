package com.github.takezoe.retry

import scala.concurrent.duration._
import java.util.concurrent.atomic.LongAdder
import java.util.concurrent.atomic.AtomicReference
import CircuitBreakerPolicy._

class CircuitBreakerPolicy(
    val failureThreshold: Int,
    val successThreshold: Int,
    val retryDuration: FiniteDuration,
    val onClose: () => Unit = () => (),
    val onOpen: (Throwable) => Unit = _ => (),
    val onHalfOpen: () => Unit = () => ()
){
    private val successCount = new LongAdder()
    private val failureCount = new LongAdder()
    private val lastFailure = new AtomicReference[(Long, Throwable)]()
    private val state = new AtomicReference[State](Close)

    def failed(e: Throwable): Unit = {
        lastFailure.set((System.currentTimeMillis, e))
        failureCount.increment()
        successCount.reset()

        // if failureThreashod <= 0 then the circuit breaker never closes.
        if (failureThreshold > 0) {
            state.get() match {
                case Close => 
                    if (failureCount.intValue() >= failureThreshold) {
                        state.set(Open)
                        onOpen(e)
                    }
                case Open =>
                    // Nothing to do
                case HalfOpen => 
                    state.set(Open)
                    onOpen(e)
            }
        } 
    }

    def succeeded(): Unit = {
        lastFailure.set(null)
        successCount.increment()
        failureCount.reset()

        // if failureThreashod <= 0 then the circuit breaker never closes.
        if (failureThreshold > 0) {
            state.get() match {
                case Close =>
                    // Nothing to do
                case Open =>
                    if (successCount.intValue() >= successThreshold) {
                        state.set(Close)
                        onClose()
                    } else {
                        state.set(HalfOpen)
                        onHalfOpen()
                    }
                case HalfOpen =>
                    if (successCount.intValue() >= successThreshold) {
                        state.set(Close)
                        onClose()
                    }
            }
        }
    }

    def getState(): (State, Option[(Long, Throwable)]) = {
        (state.get(), Option(lastFailure.get()))
    }
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
        onClose: () => Unit = () => (),
        onOpen: (Throwable) => Unit = _ => (),
        onHalfOpen: () => Unit = () => ()        
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