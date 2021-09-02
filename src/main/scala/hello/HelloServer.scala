package hello

import cats.effect.{IO, IOApp}
import cats.effect.kernel.Resource
import io.grpc.ServerServiceDefinition
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import org.slf4j.LoggerFactory
import protos.hello.GreeterFs2Grpc
import fs2.grpc.syntax.all.*

object HelloServer extends IOApp.Simple {
  val logger = LoggerFactory.getLogger("hello-server")

  val helloService: Resource[IO, ServerServiceDefinition] =
    GreeterFs2Grpc.bindServiceResource[IO](GreeterImpl())

  def startServer(service: ServerServiceDefinition): IO[Nothing] =
    NettyServerBuilder
      .forPort(9999)
      .addService(service)
      .resource[IO]
      .evalMap { server =>
        IO.delay(logger.info("Server started")) *>
          IO(server.start())
      }
      .useForever

  val run: IO[Unit] = helloService.use(startServer)
}
