package example

import frolic._
import frolic.route.Router
import frolic.route.Target
import java.lang.reflect.Method
import frolic.uri.AbsPathAndQuery
import frolic.uri.AbsPath
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import sun.net.httpserver.ServerConfig
import frolic.server.netty.NettyServer
import frolic.route.RoutingDispatcher
import frolic.server.ServerConfig

class MyController {
  def index = "hello"
}

class MyControllerTargets(myController: MyController) {
  def index = new Target {
    override def call(rh: RequestHeader) = Future { StatusAndBody(200, myController.index) }
  }
}

class MyControllerReverse(router: Router) {
  private val indexMethod = classOf[MyController].getMethod("index")
  def index = router.reverse(indexMethod, Seq.empty)
}

class MyRouter(myControllerTargets: MyControllerTargets) extends Router {
  def lookup(requestHeader: RequestHeader): Option[Target] = requestHeader.path match {
    case AbsPath(Seq()) => Some(myControllerTargets.index)
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
    val myController = new MyController
    val myControllerTargets = new MyControllerTargets(myController)
    val router = new MyRouter(myControllerTargets)
    //val myControllerReverse = new MyControllerReverse(router)
    val emptyDispatcher = new EmptyRequestDispatcher
    val dispatcher = new RoutingDispatcher(router, fallback = emptyDispatcher)
    val serverConfig = ServerConfig(port = 9000)
        val server = new NettyServer(serverConfig, dispatcher)
    server.start()
  }
}