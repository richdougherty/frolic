package frolic.route

import frolic.uri._
import java.lang.reflect.Method
import scala.collection.immutable
import frolic.RequestHeader
import frolic.RequestHandler
import frolic.RequestDispatcher
import scala.concurrent.Future

class RoutingDispatcher(router: Router, fallback: RequestDispatcher) extends RequestDispatcher {
  override def dispatch(requestHeader: RequestHeader): Future[RequestHandler] = {
    println(requestHeader)
    router.lookup(requestHeader).map { target =>
      println("calling target")
      target.call(requestHeader)
    } getOrElse {
      println("calling fallback")
      fallback.dispatch(requestHeader)
    }
  }
}

trait Router {
  def lookup(requestHeader: RequestHeader): Option[Target]
  def reverse(m: Method, args: Seq[Any]): Option[AbsPathAndQuery]
}

trait Target {
  def call(requestHeader: RequestHeader): Future[RequestHandler]
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