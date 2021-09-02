package hello

import cats.effect.IO
import io.grpc.Metadata
import protos.hello.{GreeterFs2Grpc, HelloReply, HelloRequest}

class GreeterImpl extends GreeterFs2Grpc[IO, Metadata] {
  override def sayHello(request: HelloRequest, ctx: Metadata): IO[HelloReply] =
    IO(HelloReply(message = "Hello " + request.name))
}
