package frolic.inject

import javax.inject.Provider

final class ProviderCell[T] extends Provider[T] {
  private var value: Option[T] = None
  def set(v: T) = {
    if (value.isDefined) throw new IllegalStateException("ProviderCell already has a value")
    value = Some(v)
  }
  def get(): T = value.getOrElse(throw new IllegalStateException("ProviderCell doesn't have a value"))
}