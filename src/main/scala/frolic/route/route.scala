package frolic.route

import frolic.uri._
import java.lang.reflect.Method
import scala.collection.immutable
import frolic.RequestHeader
import frolic.RequestHandler
import frolic.dispatch.Dispatcher
import scala.concurrent.Future
import frolic.dispatch.PartialDispatcher

trait Router extends PartialDispatcher {
  def reverse(m: Method, args: Seq[Any]): Option[AbsPathAndQuery]
}

// Proof of concept for wrapping an existing Router and prefixing it with some path segments
final class PrefixRouter(prefix: immutable.Seq[PathSegment], delegate: Router) extends Router {
  assert(!prefix.isEmpty)
  private def prefixPath(p: AbsPath) = AbsPath(prefix ++ p.segments)
  override def dispatch(rh: RequestHeader): Option[RequestHandler] = {
    val newPath = prefixPath(rh.path)
    delegate.dispatch(RequestHeader(newPath))
  }
  override def reverse(m: Method, args: Seq[Any]): Option[AbsPathAndQuery] = {
    delegate.reverse(m, args).map { orig: AbsPathAndQuery =>
      val newPath = prefixPath(orig.p)
      AbsPathAndQuery(newPath, orig.q)
    }
  }
}