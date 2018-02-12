# scala-retry

Offers simple retry functionality for Scala.

```scala
resolvers += "sonatype-oss-snapshot" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies += "com.github.takezoe" %% "scala-retry" % "0.0.1-SNAPSHOT"
```

## Retry synchronously

```scala
import com.github.takezoe.retry._
import scala.concurrent.duration._

implicit val config = RetryConfig(maxAttempts = 3, retryDuration = 1.seconds, backOff = LinerBackOff)

val result: String = retryBlocking {
  // something to retry
  "Hello World!"
}
```

## Retry asynchronously

```scala
import com.github.takezoe.retry._
import scala.concurrent.duration._
import scala.concurrent.Future

implicit val rm = new RetryManager()
implicit val config = RetryConfig(maxAttempts = 3, retryDuration = 1.seconds, backOff = LinerBackOff)

val future: Future[String] = retryAsync {
  // something to retry
  "Hello World!"
}
```

## Retry Future

```scala
import com.github.takezoe.retry._
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

implicit val rm = new RetryManager()
implicit val config = RetryConfig(maxAttempts = 3, retryDuration = 1.seconds, backOff = LinerBackOff)

val future: Future[String] = retryFuture {
  Future {
    // something to retry
    "Hello World!"
  }
}

