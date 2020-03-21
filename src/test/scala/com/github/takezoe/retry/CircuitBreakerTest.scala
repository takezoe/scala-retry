package com.github.takezoe.retry

import org.scalatest.FunSuite
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class CircuitBreakerTest extends FunSuite {

    test("circuitBreaker") {
        implicit val policy = CircuitBreakerPolicy(
            failureThreshold = 3,
            successThreshold = 2,
            retryDuration = 1.second,
            onOpen = (e) => println(s"open: ${e.toString}"),
            onClose = () => println("close"),
            onHalfOpen = () => println("halfOpen")
        )

        assert(circuitBreaker { "OK" } == "OK")

        assertThrows[RuntimeException] {
            circuitBreaker { throw new RuntimeException() }
        }
        assertThrows[RuntimeException] {
            circuitBreaker { throw new RuntimeException() }
        }

        // -> Open
        assertThrows[RuntimeException] {
            circuitBreaker { throw new RuntimeException() }
        }
        assertThrows[RuntimeException] {
            circuitBreaker { "OK" }
        }
        
        Thread.sleep(1000)
        // -> HalfOpen
        assert(circuitBreaker { "OK" } == "OK")

        // -> Close
        assert(circuitBreaker { "OK" } == "OK")
        assertThrows[RuntimeException] {
            circuitBreaker { throw new RuntimeException() }
        }
        assert(circuitBreaker { "OK" } == "OK")
    }

}