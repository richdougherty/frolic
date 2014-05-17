package frolic

import frolic.server.ServerConfig
import frolic.server.netty.NettyServer
import scala.concurrent.Future

object Main {
  def main(args: Array[String]): Unit = {
    val dispatcher = new RequestDispatcher {
      def dispatch(requestHeader: RequestHeader): Future[RequestHandler] = {
        Future.successful(StatusAndBody(200, "Hello world!"))
      }
    }
    val serverConfig = ServerConfig(port = 9000)
    val server = new NettyServer(serverConfig, dispatcher)
    server.start()
  }
}