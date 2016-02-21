package com.monsanto.arch.awsutil.impl

import java.util
import java.util.concurrent.{Callable, ExecutorService, TimeUnit}

/** An `ExecutorService` that wraps another executor service but does not pass down any requests to shut the service
  * down.
  */
class ShutdownFreeExecutorServiceWrapper(wrapped: ExecutorService) extends ExecutorService {
  override def shutdown() = ()

  override def isTerminated = wrapped.isTerminated

  override def awaitTermination(timeout: Long, unit: TimeUnit) = true

  override def shutdownNow() = new util.ArrayList[Runnable]()

  override def invokeAll[T](tasks: util.Collection[_ <: Callable[T]]) = wrapped.invokeAll(tasks)

  override def invokeAll[T](tasks: util.Collection[_ <: Callable[T]], timeout: Long, unit: TimeUnit) =
    wrapped.invokeAll(tasks, timeout, unit)

  override def invokeAny[T](tasks: util.Collection[_ <: Callable[T]]) = wrapped.invokeAny(tasks)

  override def invokeAny[T](tasks: util.Collection[_ <: Callable[T]], timeout: Long, unit: TimeUnit) =
    wrapped.invokeAny(tasks, timeout,unit)

  override def isShutdown = wrapped.isShutdown

  override def submit[T](task: Callable[T]) = wrapped.submit(task)

  override def submit[T](task: Runnable, result: T) = wrapped.submit(task, result)

  override def submit(task: Runnable) = wrapped.submit(task)

  override def execute(command: Runnable) = wrapped.execute(command)
}
