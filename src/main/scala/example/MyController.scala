package example

import frolic.route.Router
import java.lang.reflect.Method
import frolic.uri.AbsPathAndQuery
import frolic.uri.AbsPath
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import frolic.server.netty.NettyServer
import frolic.server.ServerConfig
import frolic.dispatch.EmptyDispatcher
import frolic.dispatch.FallbackDispatcher
import javax.inject.Provider
import frolic.inject.ProviderCell
import frolic.StatusAndBody
import frolic.RequestHeader
import frolic.RequestHandler

class MyController {
  def index = "hello"
}

class MyControllerHandlers(myController: MyController) {
  def index = StatusAndBody(200, myController.index)
}

class MyControllerReverse(router: Provider[Router]) {
  private val indexMethod = classOf[MyController].getMethod("index")
  def index = router.get.reverse(indexMethod, Seq.empty)
}

class MyRouter(myControllerHandlers: MyControllerHandlers) extends Router {
  def dispatch(requestHeader: RequestHeader): Option[RequestHandler] = requestHeader.path match {
    case AbsPath(Seq()) => Some(myControllerHandlers.index)
    case _ => None
  }

  private val indexMethod = classOf[MyController].getMethod("index")
  def reverse(m: Method, args: Seq[Any]): Option[AbsPathAndQuery] = m match {
    case `indexMethod` => Some(AbsPathAndQuery.decode("/"))
    case _ => None
  }
}

object MyMain {
  def main(args: Array[String]): Unit = {
    val routerProvider = new ProviderCell[Router]
    val myController = new MyController
    val myControllerHandlers = new MyControllerHandlers(myController)
    val router = new MyRouter(myControllerHandlers)
    routerProvider.set(router)
    val myControllerReverse = new MyControllerReverse(routerProvider)
    val emptyDispatcher = new EmptyDispatcher
    val dispatcher = new FallbackDispatcher(router, emptyDispatcher)
    val serverConfig = ServerConfig(port = 9000)
    val server = new NettyServer(serverConfig, dispatcher)
    server.start()
  }
}