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

//trait SimpleRouter {
//  final override def route(requestHeader: RequestHeader): Option[RequestHandler] = {
//    
//  }
//  def route(method: String, path: AbsPathAndQuery): Option[AbsPathAndQuery]
//  def reverse(m: Method, args: Seq[Any]): Option[AbsPathAndQuery]
//}

//
//final case class FixedPattern(method: String, path: Vector[String]) extends Route
//
//trait Route {
//  def apply()
//}
//
//trait ReverseRoute {
//}