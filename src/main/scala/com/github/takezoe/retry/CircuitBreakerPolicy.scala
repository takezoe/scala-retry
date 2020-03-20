package com.github.takezoe.retry

import scala.concurrent.duration._
import java.util.concurrent.atomic.LongAdder
import java.util.concurrent.atomic.AtomicReference
import CircuitBreakerPolicy._

class CircuitBreakerPolicy(
    val failureThreshold: Int,
    val successThreshold: Int,
    val retryDuration: FiniteDuration
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
                    }
                case Open =>
                    // Nothing to do
                case HalfOpen => 
                    state.set(Open)                    
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
                    } else {
                        state.set(HalfOpen)
                    }
                case HalfOpen =>
                    if (successCount.intValue() >= successThreshold) {
                        state.set(Close)
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

}