package frolic.dispatch

import frolic.RequestHeader
import scala.concurrent.Future
import frolic.RequestHandler
import frolic.StatusAndBody

trait Dispatcher {
  def dispatch(requestHeader: RequestHeader): Future[RequestHandler]
}

trait PartialDispatcher {
  def dispatch(requestHeader: RequestHeader): Option[Future[RequestHandler]]
}

class FallbackDispatcher(first: PartialDispatcher, second: Dispatcher) extends Dispatcher {
  def dispatch(requestHeader: RequestHeader): Future[RequestHandler] = {
    first.dispatch(requestHeader) getOrElse second.dispatch(requestHeader)
  }
}

class EmptyDispatcher extends Dispatcher {
  override def dispatch(requestHeader: RequestHeader): Future[RequestHandler] = {
    Future.successful(StatusAndBody(404, "Resource not found"))
  }
}