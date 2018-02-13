package com.github.takezoe.retry

sealed trait BackOff {
  def nextDuration(count: Int, duration: Long): Long
}

object LinerBackOff extends BackOff {
  override def nextDuration(count: Int, duration: Long): Long = duration * count
}

object ExponentialBackOff extends BackOff {
  override def nextDuration(count: Int, duration: Long): Long = duration ^ count
}

object FixedBackOff extends BackOff {
  override def nextDuration(count: Int, duration: Long): Long = duration
}