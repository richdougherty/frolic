package frolic

import scala.concurrent.{ Future, Promise }
import scala.util.{ Failure, Success, Try }

trait RequestDispatcher {
  def dispatch(requestHeader: RequestHeader): Future[RequestHandler]
}

final case class RequestHeader(path: String)

object Main {
  def main(args: Array[String]): Unit = {
    val dispatcher = new RequestDispatcher {
      def dispatch(requestHeader: RequestHeader): Future[RequestHandler] = {
        Future.successful(SendStatusAndBody(200, "Hello world!"))
      }
    }
    val server = new NettyServer(dispatcher)
    server.start()
  }
}

trait Server {
  def start(): Unit
  //def stop(): Unit
}

class NettyServer(dispatcher: RequestDispatcher) extends Server {

  import io.netty.bootstrap.ServerBootstrap
  import io.netty.buffer.Unpooled
  import io.netty.channel._
  import io.netty.channel.nio.NioEventLoopGroup
  import io.netty.channel.socket.nio.NioServerSocketChannel
  import io.netty.channel.socket.SocketChannel
  import io.netty.handler.codec.http._
  import io.netty.handler.codec.http.HttpHeaders._
  import io.netty.handler.codec.http.HttpHeaders.Names._
  import io.netty.handler.codec.http.HttpResponseStatus._
  import io.netty.handler.codec.http.HttpVersion._
  import io.netty.handler.stream.ChunkedWriteHandler
  import io.netty.util.CharsetUtil

  override def start(): Unit = {
    println("starting")

    val port = 8080
    // Configure the server.
    val bossGroup = new NioEventLoopGroup(1);
    val workerGroup = new NioEventLoopGroup();
    try {
      val b = new ServerBootstrap();
      b.option(ChannelOption.SO_BACKLOG, 1024: Integer);
      b.group(bossGroup, workerGroup)
        .channel(classOf[NioServerSocketChannel])
        .childHandler(channelInitializer);

      val ch = b.bind(port).sync().channel();
      ch.closeFuture().sync();
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }

  }

  private def channelInitializer = {
    new ChannelInitializer[SocketChannel] {
      override def initChannel(ch: SocketChannel) = {
        val p = ch.pipeline();

        // Uncomment the following line if you want HTTPS
        //SSLEngine engine = SecureChatSslContextFactory.getServerContext().createSSLEngine();
        //engine.setUseClientMode(false);
        //p.addLast("ssl", new SslHandler(engine));

        p.addLast("codec", new HttpServerCodec());
        p.addLast("handler", dispatchHandler);
      }
    }
  }

  private def dispatchHandler = {
    import scala.concurrent.ExecutionContext.Implicits.global
    new ChannelInboundHandlerAdapter {
      val CONTENT = Array[Byte]('H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd')

      override def channelReadComplete(ctx: ChannelHandlerContext) {
        //ctx.flush();
      }

      override def channelRead(ctx: ChannelHandlerContext, msg: Object) {
        msg match {
          case req: HttpRequest =>
            if (is100ContinueExpected(req)) {
              ctx.write(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE));
            }
            val keepAlive = isKeepAlive(req);

            val requestHeader = RequestHeader(path = req.getUri)

            dispatcher.dispatch(requestHeader).onComplete { trh: Try[RequestHandler] =>
              val handler: SendStatusAndBody = trh match {
                case Success(s: SendStatusAndBody) => s
                case other => SendStatusAndBody(INTERNAL_SERVER_ERROR.code, "Error")
              }
              val content = Unpooled.copiedBuffer(handler.body, CharsetUtil.UTF_8)
              val response = new DefaultFullHttpResponse(HTTP_1_1, OK, content)
              response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
              response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
              if (!keepAlive) {
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
              } else {
                response.headers().set(CONNECTION, Values.KEEP_ALIVE);
                ctx.writeAndFlush(response);
              }
            }
          case _ =>
        }
      }

      override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace();
        ctx.close();
      }
    }
  }

}

// An opaque type that the Server will use to respond to a Request
// A Responder is executed by a Server instance
// An application that returns a Responder should
// ensure that it returns an instance that can be
// handled by the given instance
trait RequestHandler

final case class SendStatusAndBody(statusCode: Int, body: String) extends RequestHandler

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
