package frolic.server.netty

import frolic._
import frolic.server.{ Server, ServerConfig }
import scala.concurrent.{ Future, Promise }
import scala.util.{ Failure, Success, Try }
import frolic.RequestHeader
import frolic.uri.AbsPath
import frolic.dispatch.Dispatcher
import javax.inject.Inject

class NettyServer @Inject() (serverConfig: ServerConfig, dispatcher: Dispatcher) extends Server {

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

    // Configure the server.
    val bossGroup = new NioEventLoopGroup(1);
    val workerGroup = new NioEventLoopGroup();
    try {
      val b = new ServerBootstrap();
      b.option(ChannelOption.SO_BACKLOG, 1024: Integer);
      b.group(bossGroup, workerGroup)
        .channel(classOf[NioServerSocketChannel])
        .childHandler(channelInitializer);

      val ch = b.bind(serverConfig.port).sync().channel();
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

            val requestHeader = RequestHeader(path = AbsPath.decode(req.getUri))

            Future(dispatcher.dispatch(requestHeader)).onComplete { trh: Try[RequestHandler] =>
              val handler: StatusAndBody = trh match {
                case Success(s: StatusAndBody) => s
                case other => StatusAndBody(INTERNAL_SERVER_ERROR.code, "Error")
              }
              println(s"Handler: $handler")
              val status = OK
              val content = Unpooled.copiedBuffer(handler.body, CharsetUtil.UTF_8)
              val response = new DefaultFullHttpResponse(HTTP_1_1, status, content)
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