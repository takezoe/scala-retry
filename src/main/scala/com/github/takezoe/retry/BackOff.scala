package com.github.takezoe.retry

sealed trait BackOff extends java.io.Serializable {
  def nextDuration(count: Int, duration: Long): Long
}

object LinerBackOff extends BackOff {
  override def nextDuration(count: Int, duration: Long): Long = duration * count
}

object ExponentialBackOff extends BackOff {
  override def nextDuration(count: Int, duration: Long): Long = duration * scala.math.pow(2, count).toLong
}

object FixedBackOff extends BackOff {
  override def nextDuration(count: Int, duration: Long): Long = duration
}
