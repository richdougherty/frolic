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
import com.google.inject.AbstractModule
import com.google.inject.Provides
import frolic.dispatch.Dispatcher
import frolic.server.Server
import com.google.inject.Singleton
import com.google.inject.Guice
import javax.inject.Inject
import frolic.inject.AppScoped
import frolic.inject.CachingAndClosingScope
import com.google.inject.name.Names
import com.google.inject.Key
import frolic.inject.AppScope

@AppScoped
class MyController {
  def index = "hello"
}

@AppScoped
class MyControllerHandlers @Inject() (myController: MyController) {
  def index = StatusAndBody(200, myController.index)
}

@AppScoped
class MyControllerReverse @Inject() (router: Provider[Router]) {
  private val indexMethod = classOf[MyController].getMethod("index")
  def index = router.get.reverse(indexMethod, Seq.empty)
}

@AppScoped
class MyRouter @Inject() (myControllerHandlers: MyControllerHandlers) extends Router {
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

class MyModule extends AbstractModule {

  override def configure = {
    {
      val appScope = new AppScope()
      bindScope(classOf[AppScoped], appScope)
      bind(classOf[AppScope]).toInstance(appScope)
    }

    bind(classOf[ServerConfig]).toInstance(ServerConfig(port = 9000))
    bind(classOf[Server]).to(classOf[NettyServer]).in(classOf[Singleton])
    bind(classOf[Router]).to(classOf[MyRouter]).in(classOf[Singleton])
  }
  
  @Provides @AppScoped
  def dispatcher(router: Router): Dispatcher = {
    val emptyDispatcher = new EmptyDispatcher
    new FallbackDispatcher(router, emptyDispatcher)
  }
}

class DevServerModule extends AbstractModule {
  override def configure = {
    bind(classOf[ServerConfig]).toInstance(ServerConfig(port=9000))
    bind(classOf[Server]).to(classOf[NettyServer]).in(classOf[Singleton])
    bind(classOf[Router]).to(classOf[MyRouter]).in(classOf[Singleton])
  }
}

object MyMain {
  def main(args: Array[String]): Unit = {
    val injector = Guice.createInjector(new DevServerModule, new MyModule)
    val server = injector.getInstance(classOf[Server])
    val appScope = injector.getInstance(classOf[AppScope])
    server.start()
    appScope.close() // Probably not called, but at least it compiles!
  }
}