# scala-retry [![CI](https://github.com/takezoe/scala-retry/workflows/CI/badge.svg)](https://github.com/takezoe/scala-retry/actions?query=branch%3Amaster) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.takezoe/scala-retry_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.takezoe/scala-retry_2.12)

Offers simple retry functionality for Scala.

```scala
libraryDependencies += "com.github.takezoe" %% "scala-retry" % "0.0.4"
```

## Retry synchronously

`retry` runs and retries a given block on the current thread. If the block is successful, it returns a value. Otherwise, it throws an exception. Note that the current thread is blocked during retrying.

```scala
import com.github.takezoe.retry._
import scala.concurrent.duration._

implicit val policy = RetryPolicy(
  maxAttempts = 3, 
  retryDuration = 1.second, 
  backOff = ExponentialBackOff, // default is FixedBackOff
  jitter = 1.second // default is no jitter
)

val result: String = retry {
  // something to retry
  "Hello World!"
}
```

## Retry Future

`retryFuture` takes `Future` (a block which generates `Future`, more precisely) instead of a function. Note that it requires `ExecutionContext` additionally.

```scala
import com.github.takezoe.retry._
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

implicit val rm = new RetryManager()
implicit val policy = RetryPolicy(
  maxAttempts = 3, 
  retryDuration = 1.second, 
  backOff = ExponentialBackOff
)

val future: Future[String] = retryFuture {
  Future {
    // something to retry
    "Hello World!"
  }
}
```

## CircuitBreaker

```scala
import com.github.takezoe.retry._
import scala.concurrent.duration._

implicit val policy = CircuitBreakerPolicy(
  failureThreshold = 3,
  successThreshold = 3,
  retryDuration = 1.minute
)

val result: String = circuitBreaker {
  // Something can be failed
  "Hello World!"
}
```
