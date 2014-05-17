package frolic.server


trait Server {
  def start(): Unit
  //def stop(): Unit
}

final case class ServerConfig(port: Int)