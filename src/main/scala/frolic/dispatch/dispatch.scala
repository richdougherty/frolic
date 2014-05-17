package frolic.dispatch

import frolic.RequestHeader
import scala.concurrent.Future
import frolic.RequestHandler
import frolic.StatusAndBody

trait Dispatcher {
  def dispatch(requestHeader: RequestHeader): RequestHandler
}

trait PartialDispatcher {
  def dispatch(requestHeader: RequestHeader): Option[RequestHandler]
}

class FallbackDispatcher(first: PartialDispatcher, second: Dispatcher) extends Dispatcher {
  def dispatch(requestHeader: RequestHeader): RequestHandler = {
    first.dispatch(requestHeader) getOrElse second.dispatch(requestHeader)
  }
}

class EmptyDispatcher extends Dispatcher {
  override def dispatch(requestHeader: RequestHeader): RequestHandler = {
    StatusAndBody(404, "Resource not found")
  }
}