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
import com.google.inject.Module
import scala.collection.immutable
import com.google.inject.Injector
import frolic.route.dsl.DslRouter
import frolic.route.dsl.RouteRule

@AppScoped
class MyController {
  def index = "hello"
}

//@AppScoped
//class MyControllerHandlers @Inject() (myController: MyController) {
//  def index = StatusAndBody(200, myController.index)
//}

//@AppScoped
//class MyControllerReverse @Inject() (router: Provider[Router]) {
//  private val indexMethod = classOf[MyController].getMethod("index")
//  def index = router.get.reverse(indexMethod, Seq.empty)
//}
//
//@AppScoped
//class MyRouter @Inject() (myControllerHandlers: MyControllerHandlers) extends Router {
//  def dispatch(requestHeader: RequestHeader): Option[RequestHandler] = requestHeader.path match {
//    case AbsPath(Seq()) => Some(myControllerHandlers.index)
//    case _ => None
//  }
//
//  private val indexMethod = classOf[MyController].getMethod("index")
//  def reverse(m: Method, args: Seq[Any]): Option[AbsPathAndQuery] = m match {
//    case `indexMethod` => Some(AbsPathAndQuery.decode("/"))
//    case _ => None
//  }
//}

@AppScoped
class MyRouter @Inject() (injector: Injector, classLoader: ClassLoader) extends DslRouter(List(
  RouteRule(AbsPath(Nil), classOf[MyController].getName, "index")
), classLoader, injector)

class AppScopeModule extends AbstractModule {
  val appScope = new AppScope()
  
  override def configure = {
    bindScope(classOf[AppScoped], appScope)
    bind(classOf[AppScope]).toInstance(appScope)
  }
  
  def close() = appScope.close()
}

class MyModule extends AbstractModule {

  override def configure = {
    bind(classOf[Router]).to(classOf[MyRouter])
  }
  
  @Provides @AppScoped
  def requestHandler(router: Router): RequestHandler = {
    val emptyDispatcher = new EmptyDispatcher
    new FallbackDispatcher(router, emptyDispatcher)
  }
}

class DevDispatcher @Inject() (injector: Injector, appModules: AppModules) extends Dispatcher {
  val appScopeModule = new AppScopeModule
  val appInjector = injector.createChildInjector((appScopeModule +: appModules.modules): _*)
  val appScope = appScopeModule.appScope
  val appRequestHandler = appInjector.getInstance(Key.get(classOf[RequestHandler]))

  // Ignores header and always returns the same handler
  def dispatch(requestHeader: RequestHeader): RequestHandler = appRequestHandler
}

final case class AppModules(modules: immutable.Seq[Module])

object MyMain {
  def main(args: Array[String]): Unit = {
    val appModules = AppModules(List(new MyModule))
    val rootInjector = Guice.createInjector(new AbstractModule {
      override def configure = {
        bind(classOf[ClassLoader]).toInstance(getClass.getClassLoader)
      }
    })
    val devDispatcher = new DevDispatcher(rootInjector, appModules)
    val serverInjector = rootInjector.createChildInjector(new AbstractModule {
      override def configure() = {
        bind(classOf[ServerConfig]).toInstance(ServerConfig(port=9000))
        bind(classOf[Server]).to(classOf[NettyServer]).in(classOf[Singleton])
        bind(classOf[RequestHandler]).toInstance(devDispatcher)
      }
    })
    val server = serverInjector.getInstance(classOf[Server])
    server.start()
    // TODO: stop server and dev mode
  }
}