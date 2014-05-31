package frolic.route.dsl

import com.google.inject.Injector
import frolic.RequestHandler
import frolic.RequestHeader
import frolic.StatusAndBody
import frolic.dispatch.PartialDispatcher
import frolic.route.Router
import frolic.uri.AbsPath
import frolic.uri.AbsPathAndQuery
import java.lang.reflect.Method

final case class RouteRule(p: AbsPath, className: String, methodName: String)

class DslRouter(rules: List[RouteRule], classLoader: ClassLoader, injector: Injector) extends Router {
  val boundRules: Seq[BoundRouteRule] = rules.map {
    case RouteRule(p, className, methodName) =>
      val clazz = classLoader.loadClass(className)
      val controller = injector.getInstance(clazz).asInstanceOf[AnyRef]
      val method = clazz.getMethod(methodName) // TODO: Handle methods with args
      val requestHandlerMethod = RequestHandlerMethod(controller, method)
      BoundRouteRule(p, method, requestHandlerMethod)
  }
  
  def dispatch(requestHeader: RequestHeader): Option[RequestHandler] = {
    boundRules.foldLeft[Option[RequestHandler]](None) {
      case (s@Some(_), _) => s
      case (None, BoundRouteRule(p, _, handlerFactory)) if p == requestHeader.path =>
        Some(handlerFactory.invoke())
      case (None, _) => None
    }
  }
  def reverse(m: Method, args: Seq[Any]): Option[AbsPathAndQuery] = {
    boundRules.foldLeft[Option[AbsPathAndQuery]](None) {
      case (s@Some(_), _) => s
      case (None, BoundRouteRule(p, method, _)) if m == method =>
        Some(AbsPathAndQuery(p, None))
      case (None, _) => None
    }
  }
}

final class RequestHandlerMethod[A](instance: AnyRef, method: Method, resultConverter: A => RequestHandler) {
  assert(method.getDeclaringClass.isAssignableFrom(instance.getClass), s"${method.getDeclaringClass} !>: ${instance.getClass}")
  //assert(method.getReturnType.isAssignableFrom(ct.runtimeClass), s"RequestHandler !>: ${method.getReturnType}")
  def invoke(args: AnyRef*): RequestHandler = {
    val result = method.invoke(instance, args: _*).asInstanceOf[A]
    val handler = resultConverter(result)
    handler
  }
}

object RequestHandlerMethod {
  def apply(instance: AnyRef, method: Method): RequestHandlerMethod[_] = {
    val stringClass = classOf[String]
    val resultConverter: (_ => RequestHandler) = method.getReturnType match {
      case `stringClass` => ((s: String) => StatusAndBody(200, s))
      case unhandled => throw new IllegalArgumentException(s"Can't convert result of type $unhandled into a RequestHandler")
    }
    new RequestHandlerMethod(instance, method, resultConverter)
  }
}

final case class BoundRouteRule(p: AbsPath, method: Method, handlerMethod: RequestHandlerMethod[_])

