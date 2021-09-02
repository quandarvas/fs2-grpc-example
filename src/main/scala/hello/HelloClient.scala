package hello

import cats.effect.{IO, IOApp}
import cats.effect.kernel.Resource
import io.grpc.{ManagedChannel, Metadata}
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import org.slf4j.LoggerFactory
import protos.hello.{GreeterFs2Grpc, HelloRequest}
import fs2.grpc.syntax.all.*

object HelloClient extends IOApp.Simple {
  val logger = LoggerFactory.getLogger("hello-client")

  def managedChannelResource: Resource[IO, ManagedChannel] =
    NettyChannelBuilder
      .forAddress("localhost", 9999)
      .usePlaintext()
      .resource[IO]

  def request = HelloRequest(name = "World")

  def sendRequest(client: GreeterFs2Grpc[IO, Metadata]): IO[Unit] =
    client.sayHello(request, Metadata()).flatMap { reply =>
      IO.delay(logger.info(reply.message))
    }

  val run: IO[Unit] =
    managedChannelResource
      .flatMap(managedChannel => GreeterFs2Grpc.clientResource[IO, Metadata](managedChannel, identity))
      .use(sendRequest)
}
