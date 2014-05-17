package frolic

import scala.concurrent.{ Future, Promise }
import scala.util.{ Failure, Success, Try }
import frolic.uri.AbsPath



final case class RequestHeader(path: AbsPath)

// An opaque type that the Server will use to respond to a Request
// A Responder is executed by a Server instance
// An application that returns a Responder should
// ensure that it returns an instance that can be
// handled by the given instance
trait RequestHandler

final case class StatusAndBody(statusCode: Int, body: String) extends RequestHandler

// @Controller
// class Hello @Inject() {
//   def index: Result = Ok("Hello")
// }

// // generated because Hello has @Controller annotation
// class HelloResponders(io: IoContext, hello: Hello) {
//   def index: Future[Responder]
// }
// // generated because Hello has @Controller annotation
// @ForController(Hello.class)
// class HelloReverse(routeMap: RouteMap) {
//   def index: Path = {
//     val reverse = routeMap.reverse(Hello.class, "index", Seq())
//     reverse.call(Seq())
//   }
// }

// trait Route {
//   // would need to be optimised
//   def dispatch(requestHeader: RequestHeader): Option[Future[Responder]]
//   def reverse: 
//   def docs: String
// }
// // Nest a route by prefixing it
// case class PrefixedRoute {
//   def reverse: 
// }

// class RouteMap(config: Config, routes: Route)

// // Generated from routes file
// class Routes(
//   // included because a route requires Hello.index
//   hello: HelloResponders
// ) extends Router {
//   def routes: List[Route] = {
//     SimpleRoute(hello, ...)
//   }
// }



// class AppModule extends AbstractModule {

//   override def configure(): Unit = {
//     bind(classOf[Routes].to(classOf[myapp.Routes])
//   }
// }
