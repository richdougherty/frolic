package frolic.inject

import com.google.inject.Key
import com.google.inject.Provider
import com.google.inject.Scope
import java.util.concurrent.ConcurrentLinkedQueue
import java.lang.AutoCloseable

final class ProviderCell[T] extends Provider[T] {
  private var value: Option[T] = None
  def set(v: T) = {
    if (value.isDefined) throw new IllegalStateException("ProviderCell already has a value")
    value = Some(v)
  }
  def get(): T = value.getOrElse(throw new IllegalStateException("ProviderCell doesn't have a value"))
}

class AppScope extends CachingAndClosingScope

class CachingAndClosingScope extends Scope {
  scope =>

  // object to close, stored in a reverse stack so it can be processed in lifo order
  var objectsToClose: List[AutoCloseable] = Nil
  
  override def scope[T](key: Key[T], unscoped: Provider[T]): Provider[T] = {
    new Provider[T] {
      var current: Option[T] = None
      override def get(): T = scope.synchronized {
        current match {
          case None =>
            val value = unscoped.get()
            value match {
              case closeable: AutoCloseable =>
                objectsToClose = closeable :: objectsToClose
              case _ =>
            }            
            current = Some(value)
            value
          case Some(value) =>
            value
        }
      }
    }
  }
  
  def close() = synchronized {
    for (closeable <- objectsToClose) {
      closeable.close()
    }
  }

}
